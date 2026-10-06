package com.backend.modules.auth.service;

import com.backend.integration.sendgrid.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;

/**
 * Implémentation unique de {@link ActivationEmailService}.
 *
 * <p>Remplace {@code DevActivationEmailService} (supprimé) pour qu'il n'y ait
 * qu'un seul bean quel que soit le profil actif.
 *
 * <p>Comportement selon la configuration :
 * <ul>
 *   <li>Tente toujours l'envoi via {@link EmailService} (qui respecte
 *       {@code sendgrid.enabled}).</li>
 *   <li>En profil {@code dev} UNIQUEMENT, journalise également le lien brut
 *       pour faciliter les tests sans vraie clé SendGrid.</li>
 *   <li>Dans tout autre profil, le lien n'apparaît jamais dans les logs.</li>
 * </ul>
 *
 * <p>Le lien est construit UNIQUEMENT à partir de la propriété de configuration
 * {@code app.frontend-url} — jamais depuis un en-tête de requête HTTP.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivationEmailServiceImpl implements ActivationEmailService {

    private final EmailService emailService;
    private final Environment environment;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.inscription.token-expiration-heures:24}")
    private int ttlHeures;

    @Override
    public void envoyerLienActivation(String email, String nomComplet, String tokenBrut) {
        // Lien construit côté serveur uniquement — jamais depuis un header de requête
        String lien = frontendUrl + "/activation?token=" + tokenBrut;

        // En profil dev : log du lien pour travailler sans clé SendGrid réelle
        if (environment.acceptsProfiles(Profiles.of("dev"))) {
            log.info("""
                    ╔══════════════════════════════════════════════════════════════╗
                    ║  [DEV] Lien d'activation — vérifiez votre configuration     ║
                    ║  {}
                    ╚══════════════════════════════════════════════════════════════╝
                    """, lien);
        }
        // Envoi réel (async, respecte sendgrid.enabled)
        emailService.envoyerConfirmationInscriptionClient(email, nomComplet, lien, ttlHeures);
    }
}
