package com.backend.integration.n8n;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Publie des événements métier vers les webhooks n8n.
 *
 * <p>n8n est utilisé comme orchestrateur externe pour les workflows complexes
 * (ex. envoi d'emails conditionnels, intégrations tierces, rappels planifiés).
 *
 * <p>Chaque événement est posté sur {@code {n8nBaseUrl}/{eventType}} en JSON.
 *
 * <p>TODO: ajouter une signature HMAC sur les requêtes sortantes pour authentifier les webhooks.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEventPublisher {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${n8n.webhook-base-url:http://localhost:5678/webhook}")
    private String webhookBaseUrl;

    /**
     * Publie un événement générique vers n8n.
     *
     * @param eventType  type d'événement (ex. "demande-soumise")
     * @param payload    données métier à transmettre
     */
    public void publier(String eventType, Map<String, Object> payload) {
        String url = webhookBaseUrl + "/" + eventType;
        try {
            restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Événement n8n publié : type={}, url={}", eventType, url);
        } catch (Exception ex) {
            // L'intégration n8n ne doit jamais faire échouer le flux principal
            log.error("Erreur publication n8n [{}] : {}", eventType, ex.getMessage());
        }
    }

    /**
     * Publie un événement structuré avec contexte tenant.
     */
    public void publier(String eventType, String tenantId, Map<String, Object> data) {
        Map<String, Object> payload = new java.util.HashMap<>(data);
        payload.put("tenantId", tenantId);
        payload.put("timestamp", java.time.Instant.now().toString());
        publier(eventType, payload);
    }
}
