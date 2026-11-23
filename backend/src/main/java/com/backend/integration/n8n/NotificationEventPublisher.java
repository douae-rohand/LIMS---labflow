package com.backend.integration.n8n;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

/** Publie les événements métier vers les webhooks n8n après commit. */
@Slf4j
@Service
public class NotificationEventPublisher {

    private final RestClient restClient;

    @Value("${n8n.webhook-base-url:http://localhost:5678/webhook}")
    private String webhookBaseUrl;

    @Value("${n8n.webhook-secret:}")
    private String webhookSecret;

    public NotificationEventPublisher(@Qualifier("n8nRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /** Vrai si le secret partagé est renseigné : les e-mails des workflows passent alors par n8n. */
    public boolean estConfigure() {
        return webhookSecret != null && !webhookSecret.isBlank();
    }

    /** Publie un événement JSON sans laisser une panne n8n interrompre l'opération métier. */
    public void publier(String eventType, Map<String, Object> payload) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.warn("Publication n8n ignorée : secret non configuré (type={})", eventType);
            return;
        }

        try {
            restClient.post()
                    .uri(webhookBaseUrl + "/" + eventType)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Webhook-Secret", webhookSecret)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Événement n8n publié : type={}", eventType);
        } catch (Exception ex) {
            log.warn("Échec publication n8n : type={}, erreur={}", eventType,
                    ex.getClass().getSimpleName());
        }
    }

    /** Publie un événement avec le contexte tenant et l'horodatage de publication. */
    public void publier(String eventType, String tenantId, Map<String, Object> data) {
        Map<String, Object> payload = new HashMap<>(data);
        payload.put("tenantId", tenantId);
        payload.put("timestamp", java.time.Instant.now().toString());
        publier(eventType, payload);
    }
}
