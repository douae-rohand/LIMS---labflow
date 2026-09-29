package com.backend.integration.llm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

/**
 * Service d'intégration LLM basé sur Spring AI.
 *
 * <p>Utilise le {@link ChatClient} auto-configuré par Spring AI à partir de
 * {@code spring.ai.openai.*} dans {@code application.yaml}. Compatible avec
 * tout provider OpenAI-compatible (OpenAI, Ollama, vLLM, LM Studio…).
 *
 * <p>Méthodes métier à implémenter dans le cadre du module M14 :
 * <ul>
 *   <li>{@code calculerScoreAnomalie(Resultat)} — détection d'anomalies dans les résultats</li>
 *   <li>{@code genererSynthese(List<Resultat>)} — synthèse automatique des résultats d'analyse</li>
 *   <li>{@code repondreQuestion(String, String)} — assistant RAG sur la base documentaire</li>
 * </ul>
 */
@Slf4j
@Service
public class LlmClientService {

    private final ChatClient chatClient;
    private final ChatModel chatModel;

    public LlmClientService(ChatClient.Builder chatClientBuilder, ChatModel chatModel) {
        this.chatClient = chatClientBuilder.build();
        this.chatModel = chatModel;
    }

    // -------------------------------------------------------------------------
    // API de compatibilité — utilisée par IaService (M14)
    // TODO: remplacer ces méthodes génériques par les méthodes métier ci-dessous
    // -------------------------------------------------------------------------

    /**
     * Envoie un prompt au LLM et retourne la réponse en texte brut.
     * Stub de compatibilité — sera remplacé par les méthodes métier M14.
     */
    public String appelerLlm(String prompt) {
        return appelerLlm(prompt, null);
    }

    /**
     * Envoie un prompt avec un message système optionnel.
     * Stub de compatibilité — sera remplacé par les méthodes métier M14.
     */
    public String appelerLlm(String prompt, String systemPrompt) {
        log.debug("Appel LLM via Spring AI – prompt ({} chars)", prompt.length());
        try {
            var spec = chatClient.prompt();
            if (systemPrompt != null && !systemPrompt.isBlank()) {
                spec = spec.system(systemPrompt);
            }
            return spec.user(prompt).call().content();
        } catch (Exception ex) {
            log.error("Erreur appel LLM : {}", ex.getMessage());
            return "Erreur lors de l'appel au modèle IA : " + ex.getMessage();
        }
    }

    /**
     * Retourne le nom du modèle actuellement utilisé par Spring AI.
     */
    public String getModeleActif() {
        // Le nom du modèle est géré par spring.ai.openai.chat.options.model
        return chatModel.getClass().getSimpleName();
    }

    // -------------------------------------------------------------------------
    // TODO (M14) : méthodes métier à implémenter
    // -------------------------------------------------------------------------

    // TODO: calculerScoreAnomalie(Resultat resultat)
    //   → construire un prompt structuré à partir des valeurs mesurées et des normes,
    //     parser la réponse JSON du LLM, retourner un score [0.0 - 1.0]

    // TODO: genererSynthese(List<Resultat> resultats)
    //   → résumé automatique en langage naturel des résultats d'une demande,
    //     destiné à être inclus dans le rapport client

    // TODO: repondreQuestion(String question, String contexte)
    //   → assistant RAG : récupérer les chunks pertinents (vecteurs),
    //     injecter dans le prompt système, appeler chatClient
}
