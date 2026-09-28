package com.backend.integration.llm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Client HTTP vers l'API LLM (OpenAI-compatible ou modèle auto-hébergé).
 *
 * <p>Supporte les endpoints compatibles avec l'API OpenAI Chat Completions
 * ({@code POST /v1/chat/completions}). Fonctionne avec OpenAI, Ollama, LM Studio,
 * vLLM ou tout provider compatible.
 *
 * <p>TODO: ajouter support streaming (SSE), gestion des tokens, retry avec backoff.
 */
@Slf4j
@Service
public class LlmClientService {

    @Value("${llm.api-url:http://localhost:11434/v1}")
    private String apiUrl;

    @Value("${llm.api-key:}")
    private String apiKey;

    @Value("${llm.model:llama3.2}")
    private String model;

    @Value("${llm.max-tokens:2048}")
    private int maxTokens;

    @Value("${llm.temperature:0.7}")
    private double temperature;

    private final RestClient restClient;

    public LlmClientService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    /**
     * Envoie un prompt au LLM et retourne la réponse en texte brut.
     *
     * @param prompt le prompt utilisateur
     * @return réponse du modèle
     */
    public String appelerLlm(String prompt) {
        return appelerLlm(prompt, null);
    }

    /**
     * Envoie un prompt avec un message système optionnel.
     *
     * @param prompt        message utilisateur
     * @param systemPrompt  instruction système (peut être null)
     * @return réponse du modèle
     */
    @SuppressWarnings("unchecked")
    public String appelerLlm(String prompt, String systemPrompt) {
        log.debug("Appel LLM [model={}] – prompt ({} chars)", model, prompt.length());

        try {
            var messages = new java.util.ArrayList<Map<String, String>>();
            if (systemPrompt != null && !systemPrompt.isBlank()) {
                messages.add(Map.of("role", "system", "content", systemPrompt));
            }
            messages.add(Map.of("role", "user", "content", prompt));

            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "messages", messages,
                    "max_tokens", maxTokens,
                    "temperature", temperature
            );

            var response = restClient.post()
                    .uri(apiUrl + "/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            if (response != null) {
                var choices = (List<Map<String, Object>>) response.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    var message = (Map<String, String>) choices.get(0).get("message");
                    if (message != null) {
                        return message.getOrDefault("content", "");
                    }
                }
            }
            return "";
        } catch (Exception ex) {
            log.error("Erreur appel LLM : {}", ex.getMessage());
            return "Erreur lors de l'appel au modèle IA : " + ex.getMessage();
        }
    }

    /**
     * Retourne le nom du modèle actuellement configuré.
     */
    public String getModeleActif() {
        return model;
    }
}
