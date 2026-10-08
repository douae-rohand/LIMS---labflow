package com.backend.modules.auth.service;

import com.backend.integration.sendgrid.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;

/**
 * Implémentation unique (tous profils) de {@link ReinitMotDePasseEmailService}.
 *
 * <ul>
 *   <li>Envoie toujours l'e-mail via {@link EmailService} (respecte {@code sendgrid.enabled}).</li>
 *   <li>En profil {@code dev} uniquement, journalise aussi le lien brut pour les tests sans SendGrid.</li>
 *   <li>Hors profil dev, le lien n'apparaît JAMAIS dans les logs.</li>
 * </ul>
 *
 * Le lien est construit côté serveur uniquement depuis {@code app.frontend-url}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReinitMotDePasseEmailServiceImpl implements ReinitMotDePasseEmailService {

    private final EmailService emailService;
    private final Environment environment;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.reinit-mot-de-passe.token-expiration-minutes:30}")
    private int ttlMinutes;

    @Override
    public void envoyerLienReinit(String email, String nomComplet, String tokenBrut) {
        String lien = frontendUrl.replaceAll("/$", "")
                + "/reinitialiser-mot-de-passe?token=" + tokenBrut;

        // En profil dev : log du lien pour travailler sans SendGrid
        if (environment.acceptsProfiles(Profiles.of("dev"))) {
            log.info("""
                    ╔══════════════════════════════════════════════════════════════╗
                    ║  [DEV] Lien de réinitialisation mot de passe                ║
                    ║  {}
                    ╚══════════════════════════════════════════════════════════════╝
                    """, lien);
        }

        // Envoi réel (async via EmailService, respecte sendgrid.enabled)
        emailService.envoyerReinitMotDePasse(email, nomComplet, lien, ttlMinutes);
    }
}
