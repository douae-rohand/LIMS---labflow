package com.backend.modules.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Limitation de débit pour POST /api/auth/inscription et POST /api/auth/activation/renvoyer.
 *
 * <p>Compteurs gérés :
 * <ul>
 *   <li><b>par IP</b> : {@code app.inscription.rate-limit.max-par-ip} tentatives
 *       sur une fenêtre de {@code app.inscription.rate-limit.fenetre-secondes} secondes.</li>
 *   <li><b>par email</b> : {@code app.inscription.rate-limit.max-par-email} tentatives
 *       sur la même fenêtre.</li>
 *   <li><b>cooldown email (renvoi)</b> : délai minimal de {@code app.inscription.rate-limit.delai-cooldown-secondes}
 *       secondes entre deux demandes de renvoi pour le même email.</li>
 * </ul>
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

    @Value("${app.inscription.rate-limit.delai-cooldown-secondes:60}")
    private int delaiCooldownSecondes;

    @Value("${app.trust-proxy:false}")
    private boolean trustProxy;

    // ---------------------------------------------------------------------------
    // Stockage en mémoire
    // ---------------------------------------------------------------------------

    private record Compteur(AtomicInteger count, Instant expireAt) {}
    private record Cooldown(Instant disponibleA) {}

    private final ConcurrentHashMap<String, Compteur> compteurIp       = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Compteur> compteurEmail    = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Cooldown> compteurCooldown = new ConcurrentHashMap<>();

    // ---------------------------------------------------------------------------
    // API publique
    // ---------------------------------------------------------------------------

    /**
     * Vérifie et incrémente le compteur pour l'IP et l'email donnés.
     *
     * @return {@code true} si la limite est dépassée (→ 429)
     */
    public boolean estLimite(String ip, String email) {
        boolean ipLimitee   = verifierEtIncrementer(compteurIp, ip, maxParIp);
        boolean emailLimite = verifierEtIncrementer(compteurEmail, email.toLowerCase(), maxParEmail);
        return ipLimitee || emailLimite;
    }

    /**
     * Vérification & enregistrement du cooldown de renvoi (CLI-02).
     * <p>Enregistré AVANT la recherche en BDD pour TOUT email (existant ou non),
     * garantissant qu'un 429 ne confirme pas l'existence du compte.
     *
     * @return {@code true} si la limite globale est dépassée OU si l'email est en cooldown (→ 429)
     */
    public boolean estEnCooldownOuLimiteRenvoi(String ip, String emailNormalized) {
        // 1. Vérifier limite générale IP & Email
        if (estLimite(ip, emailNormalized)) {
            return true;
        }

        // 2. Vérifier et enregistrer le cooldown par email
        Instant now = Instant.now();
        String key = emailNormalized.toLowerCase();

        // compute est atomique
        Cooldown existing = compteurCooldown.compute(key, (k, current) -> {
            if (current == null || now.isAfter(current.disponibleA())) {
                // Pas de cooldown en cours : on pose le nouveau cooldown
                return new Cooldown(now.plusSeconds(delaiCooldownSecondes));
            }
            // Cooldown actif : on le conserve inchangé
            return current;
        });

        // Si le cooldown précédent était encore actif à cet instant, l'accès est refusé
        return now.isBefore(existing.disponibleA().minusSeconds(delaiCooldownSecondes));
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private boolean verifierEtIncrementer(ConcurrentHashMap<String, Compteur> map,
                                           String cle, int max) {
        Instant now = Instant.now();
        Compteur compteur = map.compute(cle, (k, existing) -> {
            if (existing == null || now.isAfter(existing.expireAt())) {
                return new Compteur(new AtomicInteger(1), now.plusSeconds(fenetreSecondes));
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
        long nbIp       = supprimerExpires(compteurIp, now);
        long nbEmail    = supprimerExpires(compteurEmail, now);
        long nbCooldown = supprimerCooldownExpires(now);
        if (nbIp > 0 || nbEmail > 0 || nbCooldown > 0) {
            log.debug("Rate-limiter inscription : {} IP, {} emails, {} cooldowns nettoyés",
                    nbIp, nbEmail, nbCooldown);
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

    private long supprimerCooldownExpires(Instant now) {
        long[] count = {0};
        compteurCooldown.entrySet().removeIf(e -> {
            if (now.isAfter(e.getValue().disponibleA())) {
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
                return xff.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
