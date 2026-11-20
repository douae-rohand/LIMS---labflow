package com.backend.modules.ia.rag;

import com.backend.integration.llm.IaIndisponibleException;
import com.backend.integration.llm.LlmClientService;
import com.backend.modules.ia.donnees.OutilsDonneesService.OutilsAssistant;
import com.backend.modules.ia.donnees.SessionOutils;
import com.backend.modules.ia.dto.AssistantDtos.MessageHistorique;
import com.backend.modules.ia.dto.AssistantDtos.QuestionRequest;
import com.backend.modules.ia.dto.AssistantDtos.ReponseAssistant;
import com.backend.modules.ia.dto.AssistantDtos.Source;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Assistant IA de LabFlow (RAG), commun à tous les espaces et aux pages publiques.
 *
 * <ol>
 *   <li>Recherche des passages proches de la question, limitée à ce que le {@link ProfilAssistant}
 *       autorise : pages de documentation de son audience et catalogues de ses laboratoires.</li>
 *   <li>Si rien n'est pertinent, réponse fixe sans appeler le modèle (économise le quota).</li>
 *   <li>Sinon, appel au modèle avec les règles, les passages, l'historique récent et la question.</li>
 * </ol>
 *
 * <p>Pour le personnel et le Super Admin, le modèle peut aussi appeler des outils de lecture des données
 * du laboratoire (CDC § 3.4, paquet {@code ia.donnees}) : totaux, références de demandes, codes d'analyses.
 * Jamais de nom de patient ni de client ; les techniciens sont envoyés sous pseudonyme {@code TECH_<id>},
 * remplacé par leur nom dans le navigateur seulement.
 */
@Slf4j
@Service
public class AssistantService {

    private static final int NB_PASSAGES = 5;
    private static final int NB_PASSAGES_MAX = 7;
    private static final int NB_SOURCES_MAX = 4;
    /** Une source n'est affichée que si son score est proche du meilleur passage. */
    private static final double ECART_SOURCE = 0.10;
    private static final int TAILLE_MAX_HISTORIQUE = 500;

    private final SimpleVectorStore vectorStore;
    private final IndexationRagService indexation;
    private final LlmClientService llm;
    private final LimiteurAssistant limiteur;
    private final String promptSysteme;
    private final String regleAvecDonnees;
    private final String regleSansDonnees;
    private final double seuilSimilarite;

    public AssistantService(SimpleVectorStore vectorStore,
                            IndexationRagService indexation,
                            LlmClientService llm,
                            LimiteurAssistant limiteur,
                            @Value("classpath:prompts/assistant-systeme.txt") Resource prompt,
                            @Value("classpath:prompts/assistant-donnees.txt") Resource promptDonnees,
                            @Value("classpath:prompts/assistant-sans-donnees.txt") Resource promptSansDonnees,
                            @Value("${labflow.assistant.seuil-similarite:0.28}") double seuilSimilarite) {
        this.vectorStore = vectorStore;
        this.indexation = indexation;
        this.llm = llm;
        this.limiteur = limiteur;
        this.seuilSimilarite = seuilSimilarite;
        try {
            this.promptSysteme = prompt.getContentAsString(StandardCharsets.UTF_8);
            this.regleAvecDonnees = promptDonnees.getContentAsString(StandardCharsets.UTF_8).strip();
            this.regleSansDonnees = promptSansDonnees.getContentAsString(StandardCharsets.UTF_8).strip();
        } catch (IOException ex) {
            throw new UncheckedIOException("Prompt de l'assistant introuvable", ex);
        }
    }

    public ReponseAssistant repondre(ProfilAssistant profil, QuestionRequest requete) {
        return repondre(profil, requete, null);
    }

    /**
     * @param outils outils de lecture des données propres au rôle, ou {@code null} (visiteur, client)
     */
    public ReponseAssistant repondre(ProfilAssistant profil, QuestionRequest requete, OutilsAssistant outils) {
        if (!indexation.estPret()) {
            throw new IaIndisponibleException(false, "L'assistant démarre. Réessayez dans quelques instants.");
        }
        limiteur.consommer(profil.cleLimite(), profil.maxParHeure());
        long debut = System.currentTimeMillis();

        List<Document> passages = rechercher(requete.question(), profil);
        // Sans outil de données, inutile d'appeler le modèle quand la documentation ne dit rien
        if (passages.isEmpty() && outils == null) {
            log.info("Assistant IA : {}, aucun passage pertinent, réponse sans appel au modèle", profil.cleLimite());
            return new ReponseAssistant(profil.sansInformation(), List.of());
        }

        String systeme = promptSysteme
                .replace("{profil}", profil.description())
                .replace("{laboratoires}", profil.laboratoires())
                .replace("{sans_information}", profil.sansInformation())
                .replace("{donnees}", outils == null ? regleSansDonnees : regleAvecDonnees);
        String reponse = llm.repondre(systeme, messageUtilisateur(requete, passages), outils == null ? null : outils.outils());

        SessionOutils session = outils == null ? null : outils.session();
        List<Source> sources = new ArrayList<>(session == null ? List.of() : session.liens());
        for (Source s : sources(reponse, passages, profil)) {
            if (sources.size() >= NB_SOURCES_MAX) break;
            // Après une lecture de données, seules les pages de documentation qui mènent quelque part restent utiles
            if (!sources.isEmpty() && s.lien() == null) continue;
            if (sources.stream().noneMatch(d -> Objects.equals(d.lien(), s.lien()) && s.prix() == null)) sources.add(s);
        }
        log.info("Assistant IA : {}, passages={}, outils={}, modèle={}, durée={} ms",
                profil.cleLimite(), passages.size(), session == null ? "aucun" : session.liens().size() + " lecture(s)",
                llm.getModeleActif(), System.currentTimeMillis() - debut);
        return new ReponseAssistant(reponse, sources,
                session == null ? java.util.Map.of() : session.noms(),
                session == null ? null : session.lecture());
    }

    // -------------------------------------------------------------------------

    /** Le filtre vient du profil, construit côté serveur : jamais depuis la requête. */
    private List<Document> rechercher(String question, ProfilAssistant profil) {
        String filtre = "tenant in " + liste(profil.tenants()) + " && audience in " + liste(profil.audiences());

        // 1. Fiches dont le code d'analyse est cité tel quel (NFS, TSH…) : prioritaires
        Map<String, Document> retenus = new LinkedHashMap<>();
        for (Document fiche : indexation.fichesCitees(question, profil.tenants())) {
            retenus.putIfAbsent(fiche.getId(), fiche);
        }
        // 2. Pages propres au profil (guide du rôle, pages visiteurs…) : les 2 meilleures passent en priorité,
        //    sinon les pages générales, plus nombreuses, les écartent
        Set<String> specifiques = profil.audiences().stream()
                .filter(a -> !ProfilsAssistantService.AUDIENCE_TOUS.equals(a))
                .collect(Collectors.toSet());
        if (!specifiques.isEmpty()) {
            vectorStore.similaritySearch(SearchRequest.builder()
                            .query(question)
                            .topK(2)
                            .similarityThreshold(seuilSimilarite)
                            .filterExpression("tenant in " + liste(profil.tenants()) + " && audience in " + liste(specifiques))
                            .build())
                    .forEach(d -> retenus.putIfAbsent(d.getId(), d));
        }
        // 3. Recherche sémantique générale (embeddings locaux)
        List<Document> semantiques = vectorStore.similaritySearch(SearchRequest.builder()
                .query(question)
                .topK(NB_PASSAGES)
                .similarityThreshold(seuilSimilarite)
                .filterExpression(filtre)
                .build());
        for (Document d : semantiques) {
            if (retenus.size() >= NB_PASSAGES_MAX) break;
            retenus.putIfAbsent(d.getId(), d);
        }
        if (log.isDebugEnabled()) {
            log.debug("Passages retenus : {}", retenus.values().stream()
                    .map(d -> (d.getScore() == null ? "code" : String.format("%.2f", d.getScore()))
                            + " " + d.getMetadata().get("titre"))
                    .toList());
        }
        return new ArrayList<>(retenus.values());
    }

    /** Valeurs déjà contrôlées (schémas, audiences fixes) : on peut les citer sans échappement. */
    private static String liste(Set<String> valeurs) {
        return valeurs.stream().map(v -> "'" + v + "'").collect(Collectors.joining(",", "[", "]"));
    }

    private static String messageUtilisateur(QuestionRequest requete, List<Document> passages) {
        StringBuilder message = new StringBuilder("<contexte>\n");
        for (int i = 0; i < passages.size(); i++) {
            message.append('[').append(i + 1).append("] ").append(passages.get(i).getText()).append("\n\n");
        }
        message.append("</contexte>\n\n");

        List<MessageHistorique> historique = requete.historique() == null ? List.of() : requete.historique();
        if (!historique.isEmpty()) {
            message.append("Échanges précédents :\n");
            for (MessageHistorique m : historique) {
                String texte = m.texte().length() > TAILLE_MAX_HISTORIQUE
                        ? m.texte().substring(0, TAILLE_MAX_HISTORIQUE) + "…" : m.texte();
                message.append("client".equals(m.role()) ? "Utilisateur : " : "Assistant : ").append(texte).append('\n');
            }
            message.append('\n');
        }
        message.append("Question : ").append(requete.question().trim());
        return message.toString();
    }

    /** Sources affichées sous la réponse ; aucune si l'assistant n'a pas trouvé l'information. */
    private static List<Source> sources(String reponse, List<Document> passages, ProfilAssistant profil) {
        if (reponse.startsWith("Je n'ai pas cette information") || reponse.equals(profil.sansInformation())) {
            return List.of();
        }
        double meilleur = passages.stream()
                .map(Document::getScore).filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue).max().orElse(0);
        // Analyse citée par son code (NFS, TSH…) : les autres fiches du catalogue ne sont pas montrées en cartes
        boolean analyseCitee = passages.stream()
                .anyMatch(d -> d.getScore() == null && "catalogue".equals(d.getMetadata().get("source")));
        Map<String, Source> uniques = new LinkedHashMap<>();
        for (Document d : passages) {
            if (uniques.size() >= NB_SOURCES_MAX) break;
            if (analyseCitee && d.getScore() != null && "catalogue".equals(d.getMetadata().get("source"))) continue;
            // Toujours affichées : fiches citées par leur code (score absent) et pages propres au profil ;
            // les autres seulement si leur score est proche du meilleur
            boolean propreAuProfil = !ProfilsAssistantService.AUDIENCE_TOUS.equals(d.getMetadata().get("audience"));
            if (d.getScore() != null && !propreAuProfil && d.getScore() < meilleur - ECART_SOURCE) continue;
            Source source = LiensAssistant.source(d, profil.espace());
            uniques.putIfAbsent(source.titre(), source);
        }
        return new ArrayList<>(uniques.values());
    }
}
