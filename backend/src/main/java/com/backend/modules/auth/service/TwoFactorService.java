package com.backend.modules.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service de gestion de l'authentification à deux facteurs (2FA).
 *
 * <p>Implémentation actuelle : OTP numérique à 6 chiffres envoyé par email,
 * stocké en mémoire avec TTL de 5 minutes.
 *
 * <p>TODO : remplacer le stockage en mémoire par Redis pour la scalabilité.
 * TODO : ajouter le support TOTP (Google Authenticator) via la librairie
 *        {@code com.warrenstrange:googleauth}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TwoFactorService {

    private static final int CODE_LENGTH = 6;
    private static final int TTL_MINUTES = 5;

    // TODO: remplacer par Redis
    private final Map<String, OtpEntry> otpStore = new ConcurrentHashMap<>();

    private final SecureRandom secureRandom = new SecureRandom();

    // -------------------------------------------------------------------------
    // Génération
    // -------------------------------------------------------------------------

    /**
     * Génère un OTP à 6 chiffres, le stocke avec un TTL et retourne le code.
     *
     * @param tokenKey clé unique identifiant la session 2FA (ex. twoFactorToken JWT)
     * @return le code OTP généré (à envoyer par email)
     */
    public String genererOtp(String tokenKey) {
        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        Instant expiry = Instant.now().plus(TTL_MINUTES, ChronoUnit.MINUTES);
        otpStore.put(tokenKey, new OtpEntry(code, expiry));
        log.debug("OTP généré pour la clé [{}], expire à {}", tokenKey, expiry);
        return code;
    }

    // -------------------------------------------------------------------------
    // Vérification
    // -------------------------------------------------------------------------

    /**
     * Vérifie le code OTP fourni par l'utilisateur.
     *
     * @param tokenKey clé de session 2FA
     * @param code     code saisi par l'utilisateur
     * @return {@code true} si le code est correct et non expiré
     */
    public boolean verifierOtp(String tokenKey, String code) {
        OtpEntry entry = otpStore.get(tokenKey);

        if (entry == null) {
            log.warn("OTP introuvable pour la clé [{}]", tokenKey);
            return false;
        }

        if (Instant.now().isAfter(entry.expiry())) {
            otpStore.remove(tokenKey);
            log.warn("OTP expiré pour la clé [{}]", tokenKey);
            return false;
        }

        boolean valide = entry.code().equals(code);
        if (valide) {
            otpStore.remove(tokenKey); // usage unique
        } else {
            log.warn("Code OTP incorrect pour la clé [{}]", tokenKey);
        }
        return valide;
    }

    /**
     * Invalide manuellement un OTP (ex. lors d'un logout ou re-demande).
     */
    public void invaliderOtp(String tokenKey) {
        otpStore.remove(tokenKey);
    }

    // -------------------------------------------------------------------------
    // Modèle interne
    // -------------------------------------------------------------------------

    private record OtpEntry(String code, Instant expiry) {}
}
