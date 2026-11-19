package com.backend.modules.ia.rag;

import com.backend.common.tenant.TenantExecutor;
import com.backend.modules.essai.entity.Essai;
import com.backend.modules.essai.entity.ExigenceEchantillon;
import com.backend.modules.essai.repository.EssaiRepository;
import com.backend.modules.essai.repository.ExigenceEchantillonRepository;
import com.backend.modules.plateforme.entity.Laboratoire;
import com.backend.modules.plateforme.repository.LaboratoireRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Construit l'index de l'assistant IA.
 *
 * <ul>
 *   <li>Documentation commune ({@code resources/rag/*.md}) : métadonnée {@code tenant = commun}.</li>
 *   <li>Catalogue de chaque laboratoire actif : une fiche par analyse et une liste récapitulative,
 *       métadonnée {@code tenant = <nom du schéma>}.</li>
 * </ul>
 *
 * <p>L'index est construit au démarrage, puis les catalogues sont relus toutes les 30 minutes ;
 * un laboratoire n'est réindexé que si son catalogue a changé. Aucune donnée personnelle
 * n'est indexée : uniquement la documentation et le catalogue public.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IndexationRagService {

    public static final String TENANT_COMMUN = "commun";

    private final SimpleVectorStore vectorStore;
    private final CorpusRag corpus;
    private final TenantExecutor tenantExecutor;
    private final LaboratoireRepository laboratoireRepository;
    private final EssaiRepository essaiRepository;
    private final ExigenceEchantillonRepository exigenceRepository;

    /** Identifiants des passages indexés, par tenant, pour pouvoir les remplacer. */
    private final Map<String, List<String>> idsParTenant = new ConcurrentHashMap<>();
    /** Empreinte du contenu indexé, par tenant : évite de recalculer ce qui n'a pas changé. */
    private final Map<String, Integer> empreinteParTenant = new ConcurrentHashMap<>();

    /** Fiches du catalogue par code d'analyse (GLY, NFS…), par tenant : complète la recherche sémantique. */
    private final Map<String, Map<String, Document>> fichesParCode = new ConcurrentHashMap<>();

    private volatile boolean pret = false;

    public boolean estPret() {
        return pret;
    }

    /**
     * Fiches dont le code d'analyse apparaît tel quel dans la question (ex. « NFS », « tsh »).
     * Les sigles courts sont mal captés par les embeddings : cette recherche exacte les complète.
     */
    public List<Document> fichesCitees(String question, Set<String> tenants) {
        Set<String> mots = Arrays.stream(question.toUpperCase(Locale.ROOT).split("[^A-Z0-9-]+"))
                .filter(m -> m.length() >= 2)
                .collect(Collectors.toSet());
        List<Document> resultat = new ArrayList<>();
        for (String tenant : tenants) {
            Map<String, Document> parCode = fichesParCode.getOrDefault(tenant, Map.of());
            for (String mot : mots) {
                Document fiche = parCode.get(mot);
                if (fiche != null) resultat.add(fiche);
            }
        }
        return resultat;
    }

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void auDemarrage() {
        reindexerTout();
    }

    @Scheduled(initialDelay = 30 * 60 * 1000L, fixedDelay = 30 * 60 * 1000L)
    public void relireCatalogues() {
        if (pret) {
            indexerCatalogues();
        }
    }

    /** Reconstruit tout l'index (documentation et catalogues). */
    public synchronized Map<String, Integer> reindexerTout() {
        long debut = System.currentTimeMillis();
        try {
            empreinteParTenant.clear();
            Map<String, Integer> bilan = new HashMap<>();
            List<Document> documentation = corpus.lire().stream()
                    .map(IndexationRagService::pageDocumentation)
                    .toList();
            bilan.put(TENANT_COMMUN, remplacer(TENANT_COMMUN, documentation));
            bilan.putAll(indexerCatalogues());
            pret = true;
            log.info("Index de l'assistant IA prêt en {} ms : {}", System.currentTimeMillis() - debut, bilan);
            return bilan;
        } catch (Exception ex) {
            log.error("Indexation de l'assistant IA impossible : {}", ex.getMessage(), ex);
            return Map.of();
        }
    }

    /** Relit le catalogue de chaque laboratoire actif et réindexe ceux qui ont changé. */
    public synchronized Map<String, Integer> indexerCatalogues() {
        Map<String, Integer> bilan = new HashMap<>();
        List<Laboratoire> laboratoires = tenantExecutor.inCentral(
                () -> laboratoireRepository.findByStatutOrderByRaisonSocialeAsc("ACTIF"));
        Set<String> actifs = laboratoires.stream().map(Laboratoire::getNomSchema).collect(Collectors.toSet());

        for (Laboratoire labo : laboratoires) {
            try {
                List<Document> fiches = tenantExecutor.inTenant(labo.getNomSchema(), () -> fichesCatalogue(labo));
                bilan.put(labo.getNomSchema(), remplacer(labo.getNomSchema(), fiches));
            } catch (Exception ex) {
                log.warn("Catalogue de {} non indexé : {}", labo.getNomSchema(), ex.getMessage());
            }
        }
        // Laboratoires désactivés depuis la dernière indexation : leurs fiches sont retirées
        for (String tenant : Set.copyOf(idsParTenant.keySet())) {
            if (!TENANT_COMMUN.equals(tenant) && !actifs.contains(tenant)) {
                remplacer(tenant, List.of());
            }
        }
        return bilan;
    }

    /** Remplace les passages d'un tenant ; renvoie le nombre de passages indexés. */
    private int remplacer(String tenant, List<Document> documents) {
        int empreinte = documents.stream().map(Document::getText).toList().hashCode();
        if (Objects.equals(empreinteParTenant.get(tenant), empreinte)) {
            return documents.size();
        }
        List<String> anciens = idsParTenant.remove(tenant);
        if (anciens != null && !anciens.isEmpty()) {
            vectorStore.delete(anciens);
        }
        fichesParCode.remove(tenant);
        if (!documents.isEmpty()) {
            vectorStore.add(documents);
            idsParTenant.put(tenant, documents.stream().map(Document::getId).toList());
            Map<String, Document> parCode = new HashMap<>();
            for (Document d : documents) {
                Object code = d.getMetadata().get("code");
                if (code != null) parCode.put(code.toString(), d);
            }
            if (!parCode.isEmpty()) fichesParCode.put(tenant, parCode);
        }
        empreinteParTenant.put(tenant, empreinte);
        return documents.size();
    }

    // -------------------------------------------------------------------------
    // Fiches du catalogue (lues dans le schéma du laboratoire)
    // -------------------------------------------------------------------------

    private List<Document> fichesCatalogue(Laboratoire labo) {
        List<Essai> essais = essaiRepository.findActifsAvecDomaine();
        if (essais.isEmpty()) return List.of();

        Map<Long, List<ExigenceEchantillon>> exigences = exigenceRepository
                .findByEssai_IdIn(essais.stream().map(Essai::getId).toList()).stream()
                .collect(Collectors.groupingBy(e -> e.getEssai().getId()));

        String nomLabo = nomLaboratoire(labo);
        String tenant = labo.getNomSchema();
        List<Document> fiches = new ArrayList<>();

        StringBuilder liste = new StringBuilder("Analyses proposées par le laboratoire " + nomLabo + " :\n");
        for (Essai essai : essais) {
            String domaine = essai.getDomaine() == null ? null : essai.getDomaine().getLibelle();
            liste.append("- ").append(essai.getCode()).append(" : ").append(essai.getDesignation());
            if (domaine != null) liste.append(" (").append(domaine).append(')');
            liste.append(", ").append(montant(essai.getTarif())).append('\n');

            StringBuilder fiche = new StringBuilder();
            fiche.append("Laboratoire ").append(nomLabo).append(" – analyse ")
                    .append(essai.getCode()).append(" : ").append(essai.getDesignation()).append(".\n");
            if (domaine != null) fiche.append("Domaine : ").append(domaine).append(".\n");
            if (StringUtils.hasText(essai.getDescription())) fiche.append("Description : ").append(essai.getDescription().trim()).append('\n');
            if (StringUtils.hasText(essai.getMethode())) fiche.append("Méthode : ").append(essai.getMethode().trim()).append(".\n");
            fiche.append("Tarif : ").append(montant(essai.getTarif())).append(".\n");
            if (essai.getDureeEstimee() != null && essai.getDureeEstimee() > 0) {
                fiche.append("Durée d'analyse estimée au laboratoire : ").append(duree(essai.getDureeEstimee()))
                        .append(" (hors délais de réception, de paiement et de validation).\n");
            }
            for (ExigenceEchantillon ex : exigences.getOrDefault(essai.getId(), List.of())) {
                fiche.append("Échantillon requis : ").append(ex.getNature());
                if (StringUtils.hasText(ex.getEtatConservation())) fiche.append(", conservation : ").append(ex.getEtatConservation());
                if (ex.getQuantiteMinimale() != null) {
                    fiche.append(", quantité minimale : ").append(ex.getQuantiteMinimale().stripTrailingZeros().toPlainString());
                    if (StringUtils.hasText(ex.getUnite())) fiche.append(' ').append(ex.getUnite());
                }
                fiche.append(".\n");
            }
            String titre = essai.getCode() + " – " + essai.getDesignation() + " (" + nomLabo + ")";
            Map<String, Object> lien = new HashMap<>(Map.of("laboratoireId", labo.getId(), "essaiId", essai.getId(),
                    "prix", montant(essai.getTarif())));
            fiches.add(document(tenant, "catalogue", ProfilsAssistantService.AUDIENCE_TOUS, titre, fiche.toString().trim(),
                    nomLabo, essai.getCode().trim().toUpperCase(Locale.ROOT), lien));
        }
        fiches.add(document(tenant, "catalogue", ProfilsAssistantService.AUDIENCE_TOUS, "Liste des analyses (" + nomLabo + ")",
                liste.toString().trim(), nomLabo, null, Map.of("laboratoireId", labo.getId())));
        return fiches;
    }

    /** Page de documentation : son audience vient de l'en-tête du fichier Markdown. */
    private static Document pageDocumentation(CorpusRag.Passage p) {
        return document(TENANT_COMMUN, p.source(), p.audience(), p.titre(), p.texte(), null, null, Map.of());
    }

    /**
     * @param liens identifiants servant à construire les liens affichés sous la réponse
     *              ({@code laboratoireId}, {@code essaiId}, {@code prix}) ; jamais envoyés au modèle
     */
    private static Document document(String tenant, String source, String audience, String titre, String texte,
                                     String laboratoire, String code, Map<String, Object> liens) {
        Map<String, Object> meta = new HashMap<>(liens);
        meta.put("tenant", tenant);
        meta.put("audience", audience);
        meta.put("source", source);
        meta.put("titre", titre);
        if (laboratoire != null) meta.put("laboratoire", laboratoire);
        if (code != null) meta.put("code", code);
        String id = UUID.nameUUIDFromBytes((tenant + "|" + audience + "|" + titre + "|" + texte)
                .getBytes(StandardCharsets.UTF_8)).toString();
        return Document.builder().id(id).text(texte).metadata(meta).build();
    }

    static String nomLaboratoire(Laboratoire labo) {
        String nom = StringUtils.hasText(labo.getRaisonSociale()) ? labo.getRaisonSociale().trim() : labo.getCode();
        return StringUtils.hasText(labo.getVille()) ? nom + " (" + labo.getVille().trim() + ")" : nom;
    }

    private static String montant(BigDecimal tarif) {
        BigDecimal valeur = tarif == null ? BigDecimal.ZERO : tarif;
        return valeur.stripTrailingZeros().toPlainString() + " MAD";
    }

    private static String duree(int minutes) {
        if (minutes < 60) return minutes + " min";
        if (minutes % 60 == 0 && minutes < 24 * 60) return (minutes / 60) + " h";
        if (minutes < 24 * 60) return (minutes / 60) + " h " + (minutes % 60) + " min";
        int jours = Math.round(minutes / (24f * 60f));
        return jours + (jours > 1 ? " jours" : " jour");
    }
}
