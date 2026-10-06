package com.backend.modules.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Service d'envoi du lien d'activation actif en profil {@code dev} uniquement.
 *
 * <p>En développement, le lien est écrit dans les logs à la place d'un vrai email.
 * Cela évite d'avoir besoin d'un serveur SMTP local et empêche toute fuite
 * accidentelle de credentials en production.
 *
 * <p><strong>Règles absolues :</strong>
 * <ul>
 *   <li>Ne jamais logger le mot de passe ni son hash.</li>
 *   <li>Ne logger que le lien d'activation (qui expire de toute façon).</li>
 * </ul>
 *
 * <p>En profil {@code prod}, ce bean n'est pas chargé — un bean {@code EmailActivationService}
 * réel (utilisant {@code JavaMailSender}) doit être fourni à la place.
 */
@Slf4j
@Service
public class DevActivationEmailService implements ActivationEmailService {

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    public void envoyerLienActivation(String email, String tokenBrut) {
        String lien = frontendUrl + "/activation?token=" + tokenBrut;
        // Le lien est loggué au niveau INFO pour être visible sans configurer DEBUG
        log.info("""
                ╔══════════════════════════════════════════════════════════════╗
                ║  [DEV] Lien d'activation pour {}
                ║  {}
                ╚══════════════════════════════════════════════════════════════╝
                """, email, lien);
    }
}
