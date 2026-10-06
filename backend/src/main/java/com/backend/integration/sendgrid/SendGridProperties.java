package com.backend.integration.sendgrid;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@Getter
@Setter
@ConfigurationProperties(prefix = "sendgrid")
public class SendGridProperties {

    /**
     * Si false, aucun appel réseau n'est tenté (les e-mails sont seulement journalisés).
     */
    private boolean enabled = true;

    /**
     * Clé API SendGrid (SG....). Ne jamais la journaliser.
     */
    private String apiKey = "";

    private String fromEmail = "no-reply@labflow.local";

    private String fromName = "LabFlow LIMS";

    private String apiBaseUrl = "https://api.sendgrid.com/v3";

    private int timeoutMs = 8000;

    public boolean clePresente() {
        if (!StringUtils.hasText(apiKey)) {
            return false;
        }
        String cle = apiKey.trim();
        return cle.startsWith("SG.")
                && cle.length() > 20
                && !cle.toUpperCase().contains("REMPLACER")
                && !cle.toUpperCase().contains("CHANGE-ME")
                && !cle.toUpperCase().contains("YOUR_API");
    }

    public boolean expediteurValide() {
        return StringUtils.hasText(fromEmail) && fromEmail.contains("@") && !fromEmail.contains(" ");
    }

    public boolean pretPourEnvoi() {
        return enabled && clePresente() && expediteurValide();
    }
}
