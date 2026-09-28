package com.backend.integration.webpush;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service d'envoi de notifications Web Push (navigateur).
 *
 * <p>Utilise le protocole Web Push (RFC 8030) avec chiffrement VAPID.
 *
 * <p>TODO: ajouter la dépendance {@code nl.martijndwars:web-push} et implémenter
 * les abonnements (subscription management) et l'envoi chiffré.
 *
 * <pre>
 * // Dépendance à ajouter dans pom.xml :
 * // &lt;dependency&gt;
 * //   &lt;groupId&gt;nl.martijndwars&lt;/groupId&gt;
 * //   &lt;artifactId&gt;web-push&lt;/artifactId&gt;
 * //   &lt;version&gt;5.1.1&lt;/version&gt;
 * // &lt;/dependency&gt;
 * </pre>
 *
 * <p>La clé publique VAPID doit être générée une fois et stockée en configuration.
 * Les clients Web s'abonnent via l'API {@code PushManager} du navigateur.
 */
@Slf4j
@Service
public class WebPushService {

    // TODO: injecter les clés VAPID depuis application.yaml
    // @Value("${webpush.vapid.public-key}")
    // private String vapidPublicKey;
    //
    // @Value("${webpush.vapid.private-key}")
    // private String vapidPrivateKey;

    /**
     * Envoie une notification Web Push à un abonné.
     *
     * @param subscriptionJson  abonnement Push sérialisé en JSON
     *                          (obtenu via {@code PushManager.subscribe()} côté client)
     * @param titre             titre de la notification
     * @param corps             corps de la notification
     * @param urlAction         URL à ouvrir au clic (peut être null)
     */
    public void envoyer(String subscriptionJson, String titre, String corps, String urlAction) {
        log.info("Web Push envoyé : titre='{}' (TODO: implémenter VAPID)", titre);
        // TODO: désérialiser subscriptionJson → Subscription
        // TODO: construire le payload JSON { title, body, url }
        // TODO: appeler PushService.sendNotification(subscription, payload, vapidKeys)
    }

    /**
     * Enregistre un abonnement Web Push pour un utilisateur.
     * TODO: persister l'abonnement en base (table push_subscription).
     */
    public void enregistrerAbonnement(Long utilisateurId, String subscriptionJson) {
        log.info("Abonnement Web Push enregistré pour userId={} (TODO: persister)", utilisateurId);
    }

    /**
     * Supprime l'abonnement Web Push d'un utilisateur.
     */
    public void supprimerAbonnement(Long utilisateurId) {
        log.info("Abonnement Web Push supprimé pour userId={} (TODO: implémenter)", utilisateurId);
    }
}
