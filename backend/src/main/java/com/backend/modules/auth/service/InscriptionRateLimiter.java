package com.backend.modules.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Limitation de débit pour POST /api/auth/inscription.
 *
 * <p>Deux compteurs indépendants :
 * <ul>
 *   <li><b>par IP</b> : {@code app.inscription.rate-limit.max-par-ip} tentatives
 *       sur une fenêtre de {@code app.inscription.rate-limit.fenetre-secondes} secondes.</li>
 *   <li><b>par email</b> : {@code app.inscription.rate-limit.max-par-email} tentatives
 *       sur la même fenêtre.</li>
 * </ul>
 *
 * <p><strong>Note développement :</strong> derrière le proxy Vite (port 5173 → 8081),
 * toutes les requêtes arrivent avec la même adresse IP (127.0.0.1 ou ::1).
 * La limite par IP est donc <em>partagée</em> entre tous les développeurs
 * qui utilisent le même proxy. Augmenter {@code max-par-ip} en profil dev
 * ou couper la limite par IP si nécessaire.
 * La propriété {@code app.trust-proxy=true} active la lecture de X-Forwarded-For
 * (à n'activer QU'EN production derrière un reverse-proxy de confiance).
 *
 * <p>Le nettoyage des entrées expirées tourne toutes les 10 minutes via {@link #nettoyerExpires()}.
 */
@Slf4j
@Component
public class InscriptionRateLimiter {

    // ---------------------------------------------------------------------------
    // Configuration
    // ---------------------------------------------------------------------------

    @Value("${app.inscription.rate-limit.max-par-ip:5}")
    private int maxParIp;

    @Value("${app.inscription.rate-limit.max-par-email:3}")
    private int maxParEmail;

    @Value("${app.inscription.rate-limit.fenetre-secondes:3600}")
    private int fenetreSecondes;

    @Value("${app.trust-proxy:false}")
    private boolean trustProxy;

    // ---------------------------------------------------------------------------
    // Stockage en mémoire (suffisant pour un seul nœud)
    // TODO : remplacer par Redis pour un déploiement multi-instances
    // ---------------------------------------------------------------------------

    private record Compteur(AtomicInteger count, Instant expireAt) {}

    private final ConcurrentHashMap<String, Compteur> compteurIp    = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Compteur> compteurEmail = new ConcurrentHashMap<>();

    // ---------------------------------------------------------------------------
    // API publique
    // ---------------------------------------------------------------------------

    /**
     * Vérifie et incrémente le compteur pour l'IP et l'email donnés.
     *
     * @return {@code true} si la limite est dépassée (→ 429)
     */
    public boolean estLimite(String ip, String email) {
        boolean ipLimitee    = verifierEtIncrementer(compteurIp,    ip,    maxParIp);
        boolean emailLimite  = verifierEtIncrementer(compteurEmail, email.toLowerCase(), maxParEmail);
        return ipLimitee || emailLimite;
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private boolean verifierEtIncrementer(ConcurrentHashMap<String, Compteur> map,
                                           String cle, int max) {
        Instant now = Instant.now();
        Compteur compteur = map.compute(cle, (k, existing) -> {
            if (existing == null || now.isAfter(existing.expireAt())) {
                // Première tentative ou fenêtre expirée : nouveau compteur
                return new Compteur(new AtomicInteger(1),
                        now.plusSeconds(fenetreSecondes));
            }
            existing.count().incrementAndGet();
            return existing;
        });
        return compteur.count().get() > max;
    }

    // ---------------------------------------------------------------------------
    // Nettoyage périodique (toutes les 10 minutes)
    // ---------------------------------------------------------------------------

    @Scheduled(fixedDelay = 600_000)
    public void nettoyerExpires() {
        Instant now = Instant.now();
        long nbIp    = supprimerExpires(compteurIp, now);
        long nbEmail = supprimerExpires(compteurEmail, now);
        if (nbIp > 0 || nbEmail > 0) {
            log.debug("Rate-limiter inscription : {} entrées IP et {} entrées email nettoyées",
                    nbIp, nbEmail);
        }
    }

    private long supprimerExpires(ConcurrentHashMap<String, Compteur> map, Instant now) {
        long[] count = {0};
        map.entrySet().removeIf(e -> {
            if (now.isAfter(e.getValue().expireAt())) {
                count[0]++;
                return true;
            }
            return false;
        });
        return count[0];
    }

    // ---------------------------------------------------------------------------
    // Lecture de l'IP (X-Forwarded-For uniquement si trust-proxy = true)
    // ---------------------------------------------------------------------------

    public String extraireIp(jakarta.servlet.http.HttpServletRequest request) {
        if (trustProxy) {
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                // Prendre la première IP de la chaîne (la plus proche du client)
                return xff.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
