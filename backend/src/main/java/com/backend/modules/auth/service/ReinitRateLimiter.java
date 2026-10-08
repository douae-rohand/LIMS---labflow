package com.backend.modules.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Limitation de débit spécifique aux endpoints de réinitialisation de mot de passe.
 *
 * <p>Compteurs <strong>entièrement séparés</strong> de {@link InscriptionRateLimiter}.
 *
 * <h3>POST /mot-de-passe/oublie</h3>
 * <ul>
 *   <li>Limite globale par IP ({@code max-par-ip} / {@code fenetre-secondes})</li>
 *   <li>Limite globale par email ({@code max-par-email} / {@code fenetre-secondes})</li>
 *   <li>Cooldown par email ({@code delai-cooldown-secondes}) : délai minimum entre deux
 *       demandes successives pour le même email. Enregistré AVANT toute recherche en base,
 *       donc le 429 ne révèle pas l'existence du compte.</li>
 * </ul>
 *
 * <h3>POST /mot-de-passe/reinitialiser</h3>
 * Limité par IP uniquement (la requête ne contient pas d'e-mail).
 */
@Slf4j
@Component
public class ReinitRateLimiter {

    // -------------------------------------------------------------------------
    // Configuration
    // -------------------------------------------------------------------------

    @Value("${app.reinit-mot-de-passe.rate-limit.max-par-ip:5}")
    private int maxParIp;

    @Value("${app.reinit-mot-de-passe.rate-limit.max-par-email:3}")
    private int maxParEmail;

    @Value("${app.reinit-mot-de-passe.rate-limit.fenetre-secondes:3600}")
    private int fenetreSecondes;

    /** Délai minimum (secondes) entre deux demandes pour le même email. Défaut : 60 s. */
    @Value("${app.reinit-mot-de-passe.rate-limit.delai-cooldown-secondes:60}")
    private int delaiCooldownSecondes;

    @Value("${app.trust-proxy:false}")
    private boolean trustProxy;

    // -------------------------------------------------------------------------
    // Stockage
    // -------------------------------------------------------------------------

    private record Compteur(AtomicInteger count, Instant expireAt) {}
    private record Cooldown(Instant disponibleA) {}

    /** Compteurs dédiés — jamais partagés avec InscriptionRateLimiter. */
    private final ConcurrentHashMap<String, Compteur> compteurIp      = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Compteur> compteurEmail   = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Cooldown> compteurCooldown = new ConcurrentHashMap<>();

    // -------------------------------------------------------------------------
    // API publique
    // -------------------------------------------------------------------------

    /**
     * Vérifie la limite globale (IP + email) ET le cooldown par email.
     * Enregistre le cooldown AVANT la recherche du compte en base.
     * Utilisé par POST /mot-de-passe/oublie.
     *
     * @return {@code true} si la limite ou le cooldown est atteint → 429
     */
    public boolean estLimiteOuCooldown(String ip, String email) {
        // 1. Vérifier les limites globales (sans cooldown, sans enregistrement)
        boolean ipLimitee   = verifierEtIncrementer(compteurIp,    ip,                  maxParIp);
        boolean emailLimite = verifierEtIncrementer(compteurEmail, email.toLowerCase(), maxParEmail);
        if (ipLimitee || emailLimite) {
            return true;
        }

        // 2. Vérifier et enregistrer le cooldown par email (atomique)
        //    Enregistré ICI — avant toute recherche de compte en base,
        //    pour que le 429 ne révèle pas l'existence du compte.
        Instant now = Instant.now();
        String cle = email.toLowerCase();

        Cooldown cooldown = compteurCooldown.compute(cle, (k, existing) -> {
            if (existing == null || now.isAfter(existing.disponibleA())) {
                // Pas de cooldown actif : enregistrer le nouveau
                return new Cooldown(now.plusSeconds(delaiCooldownSecondes));
            }
            // Cooldown actif : laisser inchangé
            return existing;
        });

        // Le cooldown était-il déjà actif avant ce compute ?
        // Si `disponibleA` est dans le futur ET qu'on vient de le créer, c'est OK.
        // Si `disponibleA` est dans le futur mais existait AVANT ce compute, c'est bloqué.
        return now.isBefore(cooldown.disponibleA().minusSeconds(delaiCooldownSecondes));
    }

    /**
     * Vérifie et incrémente uniquement le compteur IP.
     * Utilisé par POST /mot-de-passe/reinitialiser (pas d'email dans la requête).
     *
     * @return {@code true} si la limite IP est atteinte → 429
     */
    public boolean estLimiteParIp(String ip) {
        return verifierEtIncrementer(compteurIp, ip, maxParIp);
    }

    // -------------------------------------------------------------------------
    // Lecture de l'IP
    // -------------------------------------------------------------------------

    public String extraireIp(jakarta.servlet.http.HttpServletRequest request) {
        if (trustProxy) {
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                return xff.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Nettoyage périodique
    // -------------------------------------------------------------------------

    @Scheduled(fixedDelay = 600_000)
    public void nettoyerExpires() {
        Instant now = Instant.now();
        long nbIp       = supprimerExpiresCompteur(compteurIp, now);
        long nbEmail    = supprimerExpiresCompteur(compteurEmail, now);
        long nbCooldown = supprimerExpiresCooldown(now);
        if (nbIp > 0 || nbEmail > 0 || nbCooldown > 0) {
            log.debug("Rate-limiter réinitialisation : {} IP, {} emails, {} cooldowns nettoyés",
                    nbIp, nbEmail, nbCooldown);
        }
    }

    private long supprimerExpiresCompteur(ConcurrentHashMap<String, Compteur> map, Instant now) {
        long[] count = {0};
        map.entrySet().removeIf(e -> {
            if (now.isAfter(e.getValue().expireAt())) { count[0]++; return true; }
            return false;
        });
        return count[0];
    }

    private long supprimerExpiresCooldown(Instant now) {
        long[] count = {0};
        compteurCooldown.entrySet().removeIf(e -> {
            if (now.isAfter(e.getValue().disponibleA())) { count[0]++; return true; }
            return false;
        });
        return count[0];
    }
}
