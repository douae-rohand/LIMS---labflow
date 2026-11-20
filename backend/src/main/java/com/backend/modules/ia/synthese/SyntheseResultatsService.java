package com.backend.modules.ia.synthese;

import com.backend.common.exception.BusinessRuleException;
import com.backend.common.exception.ResourceNotFoundException;
import com.backend.common.security.SecurityUtils;
import com.backend.common.tenant.TenantContext;
import com.backend.common.tenant.TenantExecutor;
import com.backend.integration.llm.IaIndisponibleException;
import com.backend.integration.llm.LlmClientService;
import com.backend.modules.demande.entity.Demande;
import com.backend.modules.demande.repository.DemandeRepository;
import com.backend.modules.essai.entity.Essai;
import com.backend.modules.essai.entity.LigneEssai;
import com.backend.modules.essai.entity.StatutEssai;
import com.backend.modules.essai.repository.LigneEssaiRepository;
import com.backend.modules.ia.entity.SyntheseIa;
import com.backend.modules.ia.rag.LimiteurAssistant;
import com.backend.modules.ia.repository.SyntheseIaRepository;
import com.backend.modules.rapport.repository.RapportRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Synthèse automatique des résultats par l'IA (CDC : M07, M14, § 3.4).
 *
 * <p>Générée après la signature du rapport par le Responsable : elle ne remplace jamais la validation
 * humaine et reste affichée à part du rapport officiel, avec la mention « non contractuelle ».
 *
 * <p><b>Données envoyées au fournisseur (Groq)</b> : uniquement le code, la désignation et le domaine de
 * chaque analyse, la valeur, l'unité, les intervalles et le statut calculé ici. Jamais le nom, l'identité,
 * le titre de la demande, les motifs saisis ni aucune donnée du client (loi 09-08, CNDP).
 *
 * <p>Le statut de chaque résultat (dans l'intervalle, hors intervalle, critique) est calculé par le code,
 * pas par le modèle : le modèle ne fait que le rédiger.
 */
@Slf4j
@Service
public class SyntheseResultatsService {

    private final LigneEssaiRepository ligneEssaiRepository;
    private final DemandeRepository demandeRepository;
    private final SyntheseIaRepository syntheseIaRepository;
    private final TenantExecutor tenantExecutor;
    private final LlmClientService llm;
    private final RapportRepository rapportRepository;
    private final LimiteurAssistant limiteur;
    private final String promptSysteme;

    /** Mention affichée avec la synthèse, côté client comme côté laboratoire. */
    public static final String MENTION = "Synthèse automatique, non contractuelle";
    private static final Set<String> RAPPORT_SIGNE = Set.of("SIGNE", "ENVOYE");
    private static final int REGENERATIONS_PAR_HEURE = 20;

    public record SyntheseDto(Long demandeId, String contenu, Instant dateGeneration, String mention) {}

    public SyntheseResultatsService(LigneEssaiRepository ligneEssaiRepository,
                                    DemandeRepository demandeRepository,
                                    SyntheseIaRepository syntheseIaRepository,
                                    TenantExecutor tenantExecutor,
                                    LlmClientService llm,
                                    RapportRepository rapportRepository,
                                    LimiteurAssistant limiteur,
                                    @Value("classpath:prompts/synthese-resultats.txt") Resource prompt) {
        this.ligneEssaiRepository = ligneEssaiRepository;
        this.demandeRepository = demandeRepository;
        this.syntheseIaRepository = syntheseIaRepository;
        this.tenantExecutor = tenantExecutor;
        this.llm = llm;
        this.rapportRepository = rapportRepository;
        this.limiteur = limiteur;
        try {
            this.promptSysteme = prompt.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException("Prompt de synthèse introuvable", ex);
        }
    }

    /**
     * Appelée après le commit de la signature : ne bloque pas le Responsable et n'échoue jamais
     * bruyamment (la synthèse est facultative, elle pourra être régénérée).
     */
    @Async
    public void genererEnArrierePlan(String schema, Long demandeId) {
        try {
            generer(schema, demandeId);
        } catch (IaIndisponibleException ex) {
            log.warn("Synthèse IA de la demande {} ({}) non générée : {}", demandeId, schema, ex.getMessage());
        } catch (RuntimeException ex) {
            log.error("Synthèse IA de la demande {} ({}) en échec : {}", demandeId, schema, ex.getMessage());
        }
    }

    /**
     * Génère (ou remplace) la synthèse d'une demande et la renvoie.
     *
     * @throws IaIndisponibleException si le fournisseur est saturé ou injoignable
     */
    public String generer(String schema, Long demandeId) {
        long debut = System.currentTimeMillis();
        // 1. Lecture des résultats (transaction courte, dans le schéma du laboratoire)
        Donnees donnees = tenantExecutor.inTenant(schema, () -> lire(demandeId));
        log.debug("Synthèse IA, données anonymes envoyées :\n{}", donnees.message());

        // 2. Appel au modèle hors transaction (plusieurs secondes)
        String contenu = llm.repondre(promptSysteme, donnees.message()).trim();

        // 3. Enregistrement : une synthèse par demande, remplacée si elle existe
        tenantExecutor.inTenant(schema, () -> enregistrer(demandeId, donnees.numero(), contenu));
        log.info("Synthèse IA générée : demande={}, schéma={}, analyses={}, modèle={}, durée={} ms",
                demandeId, schema, donnees.nbAnalyses(), llm.getModeleActif(), System.currentTimeMillis() - debut);
        return contenu;
    }

    /** Synthèse actuelle d'une demande du laboratoire courant (espace du personnel). */
    public Optional<SyntheseDto> consulter(Long demandeId) {
        return syntheseIaRepository.findFirstByDemande_IdOrderByDateGenerationDesc(demandeId)
                .map(s -> new SyntheseDto(demandeId, s.getContenu(), s.getDateGeneration(), MENTION));
    }

    /**
     * Régénération à la demande du Responsable (après un échec du fournisseur, ou une correction
     * de résultat suivie d'une nouvelle signature). Seulement pour un rapport déjà signé.
     */
    public SyntheseDto regenerer(Long demandeId) {
        String schema = TenantContext.getCurrentTenant();
        if (schema == null) {
            throw new BusinessRuleException("SYNTHESE_HORS_LABORATOIRE", "Aucun laboratoire associé à ce compte.");
        }
        boolean signe = rapportRepository.findByDemande_IdInOrderByIdDesc(List.of(demandeId)).stream()
                .anyMatch(r -> RAPPORT_SIGNE.contains(r.getStatut()));
        if (!signe) {
            throw new BusinessRuleException("SYNTHESE_RAPPORT_NON_SIGNE",
                    "La synthèse n'est générée qu'après la signature du rapport.");
        }
        limiteur.consommer("synthese:u:" + SecurityUtils.principalCourant().getId(), REGENERATIONS_PAR_HEURE);
        String contenu = generer(schema, demandeId);
        return new SyntheseDto(demandeId, contenu, Instant.now(), MENTION);
    }

    // -------------------------------------------------------------------------

    private record Donnees(String numero, String message, int nbAnalyses) {}

    private Donnees lire(Long demandeId) {
        Demande demande = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande", "id", demandeId));
        List<LigneEssai> lignes = ligneEssaiRepository.findByDemande_Id(demandeId);
        List<LigneEssai> realisees = lignes.stream()
                .filter(l -> l.getStatut() != StatutEssai.NON_REALISABLE && StringUtils.hasText(l.getValeur()))
                .toList();
        if (realisees.isEmpty()) {
            throw new BusinessRuleException("SYNTHESE_SANS_RESULTAT", "Aucun résultat saisi pour cette demande.");
        }
        long nonRealisees = lignes.stream().filter(l -> l.getStatut() == StatutEssai.NON_REALISABLE).count();

        StringBuilder message = new StringBuilder("<resultats>\n");
        int critiques = 0;
        int horsIntervalle = 0;
        int normaux = 0;
        for (LigneEssai ligne : realisees) {
            message.append(ligneResultat(ligne)).append('\n');
            String statut = statut(ligne, ligne.getEssai());
            if (statut.startsWith("CRITIQUE")) critiques++;
            else if (statut.contains("dessous") || statut.equals("non conforme")) horsIntervalle++;
            else if (statut.startsWith("dans") || statut.equals("conforme")) normaux++;
        }
        // Bilan compté ici : le modèle le reprend sans recompter (il se trompe parfois dans les totaux)
        message.append("Bilan : ").append(realisees.size()).append(" analyse(s) ; ")
                .append(critiques).append(" critique(s) ; ")
                .append(horsIntervalle).append(" hors intervalle sans être critique(s) ; ")
                .append(normaux).append(" dans l'intervalle ou conforme(s) ; ")
                .append(realisees.size() - critiques - horsIntervalle - normaux).append(" sans comparaison possible.\n");
        if (nonRealisees > 0) {
            message.append("Analyses non réalisées : ").append(nonRealisees).append('\n');
        }
        message.append("</resultats>\n\nRédige la synthèse de ces résultats.");
        return new Donnees(demande.getNumero(), message.toString(), realisees.size());
    }

    /** Une ligne anonyme : analyse, domaine, valeur, unité, intervalles et statut calculé. */
    static String ligneResultat(LigneEssai ligne) {
        Essai essai = ligne.getEssai();
        String unite = StringUtils.hasText(essai.getUnite()) ? " " + essai.getUnite().trim() : "";
        StringBuilder texte = new StringBuilder("- ")
                .append(essai.getCode()).append(" – ").append(essai.getDesignation());
        if (essai.getDomaine() != null) texte.append(" (domaine : ").append(essai.getDomaine().getLibelle()).append(')');
        texte.append(" : ").append(ligne.getValeur().trim()).append(unite);
        if (essai.getLimiteMin() != null || essai.getLimiteMax() != null) {
            texte.append(" ; intervalle de référence : ").append(intervalle(essai.getLimiteMin(), essai.getLimiteMax())).append(unite);
        }
        if (essai.getSeuilCritiqueMin() != null || essai.getSeuilCritiqueMax() != null) {
            texte.append(" ; seuils critiques : ").append(intervalle(essai.getSeuilCritiqueMin(), essai.getSeuilCritiqueMax())).append(unite);
        }
        texte.append(" ; statut : ").append(statut(ligne, essai)).append('.');
        return texte.toString();
    }

    static String statut(LigneEssai ligne, Essai essai) {
        BigDecimal valeur = nombre(ligne.getValeur());
        if (valeur != null) {
            if (essai.getSeuilCritiqueMin() != null && valeur.compareTo(essai.getSeuilCritiqueMin()) < 0) {
                return "CRITIQUE (en dessous du seuil critique)";
            }
            if (essai.getSeuilCritiqueMax() != null && valeur.compareTo(essai.getSeuilCritiqueMax()) > 0) {
                return "CRITIQUE (au-dessus du seuil critique)";
            }
            if (essai.getLimiteMin() != null && valeur.compareTo(essai.getLimiteMin()) < 0) {
                return "en dessous de l'intervalle de référence";
            }
            if (essai.getLimiteMax() != null && valeur.compareTo(essai.getLimiteMax()) > 0) {
                return "au-dessus de l'intervalle de référence";
            }
            if (essai.getLimiteMin() != null || essai.getLimiteMax() != null) {
                return "dans l'intervalle de référence";
            }
        }
        // Valeur non numérique ou sans intervalle : on reprend la conformité saisie au laboratoire
        if (Boolean.TRUE.equals(ligne.getConformite())) return "conforme";
        if (Boolean.FALSE.equals(ligne.getConformite())) return "non conforme";
        if (valeur == null && (essai.getLimiteMin() != null || essai.getLimiteMax() != null)) {
            return "valeur non numérique, non comparée à l'intervalle";
        }
        return "sans intervalle de référence";
    }

    private static BigDecimal nombre(String valeur) {
        try {
            return new BigDecimal(valeur.trim().replace(',', '.').replace(" ", ""));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String intervalle(BigDecimal min, BigDecimal max) {
        if (min != null && max != null) return texte(min) + " à " + texte(max);
        if (min != null) return "au moins " + texte(min);
        return "au plus " + texte(max);
    }

    private static String texte(BigDecimal nombre) {
        return nombre.stripTrailingZeros().toPlainString();
    }

    private Void enregistrer(Long demandeId, String numero, String contenu) {
        SyntheseIa synthese = syntheseIaRepository.findFirstByDemande_IdOrderByDateGenerationDesc(demandeId)
                .orElseGet(() -> SyntheseIa.builder()
                        .code(code(numero, demandeId))
                        .demande(demandeRepository.getReferenceById(demandeId))
                        .build());
        synthese.setContenu(contenu);
        synthese.setDateGeneration(Instant.now());
        syntheseIaRepository.save(synthese);
        return null;
    }

    /** Code unique de la synthèse (50 caractères au plus). */
    private static String code(String numero, Long demandeId) {
        String code = "SYN-" + (StringUtils.hasText(numero) ? numero : String.valueOf(demandeId));
        return code.length() > 50 ? code.substring(0, 50) : code;
    }
}
