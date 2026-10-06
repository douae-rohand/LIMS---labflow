package com.backend.integration.sendgrid;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Component
public class SendGridMailClient {

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final RestClient sendGridRestClient;
    private final SendGridProperties properties;
    private final ObjectMapper objectMapper;

    public SendGridMailClient(
            @Qualifier("sendGridRestClient") RestClient sendGridRestClient,
            SendGridProperties properties,
            ObjectMapper objectMapper) {
        this.sendGridRestClient = sendGridRestClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public ResultatEnvoiEmail envoyer(List<String> destinataires, String sujet, String html, String texte) {
        List<String> propres = normaliserDestinataires(destinataires);
        if (propres.isEmpty()) {
            log.warn("Aucun destinataire valide pour le sujet [{}]", sujet);
            return ResultatEnvoiEmail.ignore("Aucun destinataire e-mail valide");
        }
        if (!properties.isEnabled()) {
            log.info("SendGrid désactivé : e-mail [{}] non envoyé à {}", sujet, propres);
            return ResultatEnvoiEmail.nonConfigure("SendGrid est désactivé (SENDGRID_ENABLED=false)");
        }
        if (!properties.clePresente()) {
            log.warn("SendGrid non configuré : clé API absente ou placeholder. E-mail [{}] non envoyé à {}",
                    sujet, propres);
            return ResultatEnvoiEmail.nonConfigure(
                    "SENDGRID_API_KEY manquante ou invalide. Ajoutez une clé SG. réelle dans backend/.env");
        }
        if (!properties.expediteurValide()) {
            log.error("SendGrid : SENDGRID_FROM_EMAIL invalide");
            return ResultatEnvoiEmail.nonConfigure("SENDGRID_FROM_EMAIL est invalide");
        }

        Map<String, Object> payload = construirePayload(propres, sujet, html, texte);
        try {
            ResponseEntity<String> reponse = sendGridRestClient.post()
                    .uri("/mail/send")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey().trim())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toEntity(String.class);
            int statut = reponse.getStatusCode().value();
            if (statut == 202 || reponse.getStatusCode().is2xxSuccessful()) {
                log.info("SendGrid a accepté l'e-mail [{}] pour {} destinataire(s) (HTTP {})",
                        sujet, propres.size(), statut);
                return ResultatEnvoiEmail.accepte(statut);
            }
            return classerErreur(statut, reponse.getBody());
        } catch (RestClientResponseException ex) {
            return classerErreur(ex.getStatusCode().value(), ex.getResponseBodyAsString());
        } catch (Exception ex) {
            log.error("SendGrid indisponible pour [{}] : {}", sujet, ex.getMessage());
            return ResultatEnvoiEmail.echec(true, 0, "ERREUR_RESEAU",
                    "Impossible de joindre SendGrid (timeout ou réseau). Réessayez plus tard.");
        }
    }

    public List<String> normaliserDestinataires(List<String> destinataires) {
        if (destinataires == null) {
            return List.of();
        }
        return destinataires.stream()
                .filter(StringUtils::hasText)
                .map(email -> email.trim().toLowerCase())
                .filter(email -> EMAIL.matcher(email).matches())
                .distinct()
                .toList();
    }

    private Map<String, Object> construirePayload(List<String> destinataires, String sujet,
                                                  String html, String texte) {
        List<Map<String, String>> to = new ArrayList<>();
        for (String email : destinataires) {
            to.add(Map.of("email", email));
        }
        Map<String, Object> personalization = new LinkedHashMap<>();
        personalization.put("to", to);

        Map<String, String> from = new LinkedHashMap<>();
        from.put("email", properties.getFromEmail().trim());
        if (StringUtils.hasText(properties.getFromName())) {
            from.put("name", properties.getFromName().trim());
        }

        List<Map<String, String>> content = new ArrayList<>();
        content.add(Map.of("type", "text/plain", "value",
                StringUtils.hasText(texte) ? texte : EmailTemplates.texteBrut(html)));
        content.add(Map.of("type", "text/html", "value", html));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("personalizations", List.of(personalization));
        payload.put("from", from);
        payload.put("subject", sujet);
        payload.put("content", content);
        payload.put("tracking_settings", Map.of(
                "click_tracking", Map.of("enable", false),
                "open_tracking", Map.of("enable", false)
        ));
        return payload;
    }

    ResultatEnvoiEmail classerErreur(int statut, String corps) {
        String messageSendGrid = extraireMessage(corps);
        boolean temporaire = statut == 429 || statut >= 500 || statut == 0;
        String code = temporaire ? "ERREUR_TEMPORAIRE" : "ERREUR_DEFINITIVE";
        log.error("SendGrid a refusé l'envoi (HTTP {}, {}) : {}", statut, code, messageSendGrid);
        return ResultatEnvoiEmail.echec(temporaire, statut, code, messageSendGrid);
    }

    private String extraireMessage(String corps) {
        if (!StringUtils.hasText(corps)) {
            return "Réponse SendGrid vide";
        }
        try {
            JsonNode root = objectMapper.readTree(corps);
            JsonNode errors = root.path("errors");
            if (errors.isArray() && !errors.isEmpty()) {
                return errors.get(0).path("message").asText("Erreur SendGrid");
            }
        } catch (Exception ignored) {
            // Corps non JSON : on ne le dump pas (peut contenir des détails internes).
        }
        return "SendGrid a renvoyé une erreur";
    }
}
