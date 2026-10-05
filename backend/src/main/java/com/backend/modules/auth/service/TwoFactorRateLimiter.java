package com.backend.modules.auth.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service de limitation du nombre de tentatives de vérification 2FA.
 *
 * <p>Règle : Maximum 5 échecs par clé (email / user) dans une fenêtre de 5 minutes.
 */
@Service
public class TwoFactorRateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_DURATION_SECONDS = 300; // 5 minutes

    private final Map<String, AttemptRecord> attempts = new ConcurrentHashMap<>();

    private record AttemptRecord(AtomicInteger count, Instant firstAttemptTime) {}

    /**
     * Vérifie si la clé donnée est actuellement bloquée pour trop d'échecs.
     *
     * @param key identifiant (ex: email)
     * @return {@code true} si bloqué
     */
    public boolean isBlocked(String key) {
        AttemptRecord record = attempts.get(key);
        if (record == null) {
            return false;
        }
        if (Instant.now().isAfter(record.firstAttemptTime().plusSeconds(LOCK_DURATION_SECONDS))) {
            attempts.remove(key);
            return false;
        }
        return record.count().get() >= MAX_ATTEMPTS;
    }

    /**
     * Enregistre un échec de vérification TOTP.
     *
     * @param key identifiant
     */
    public void recordFailure(String key) {
        Instant now = Instant.now();
        attempts.compute(key, (k, record) -> {
            if (record == null || now.isAfter(record.firstAttemptTime().plusSeconds(LOCK_DURATION_SECONDS))) {
                return new AttemptRecord(new AtomicInteger(1), now);
            }
            record.count().incrementAndGet();
            return record;
        });
    }

    /**
     * Réinitialise le compteur après un succès.
     *
     * @param key identifiant
     */
    public void resetAttempts(String key) {
        attempts.remove(key);
    }
}
