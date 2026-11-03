package com.backend.modules.auth.service;

import com.backend.common.exception.BusinessRuleException;
import com.backend.modules.utilisateur.entity.Utilisateur;
import com.backend.modules.utilisateur.repository.UtilisateurRepository;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Service de gestion de l'authentification à deux facteurs TOTP (RFC 6238).
 *
 * <p>Flux d'enrôlement :
 * <ol>
 *   <li>{@code /2fa/setup}   — génère un secret, le stocke dans {@code secret_2fa} (en attente),
 *       retourne l'URL TOTP pour le QR code. {@code double_authentification} reste à {@code false}.</li>
 *   <li>{@code /2fa/activer} — l'utilisateur soumet un premier code TOTP valide ;
 *       {@code double_authentification} passe à {@code true}.</li>
 * </ol>
 *
 * <p>Flux de vérification (login) :
 * <ol>
 *   <li>Login → 2FA activée → token temporaire de type {@code 2fa} retourné.</li>
 *   <li>{@code /2fa/verify} — code TOTP vérifié avec {@link #verifierTotp(String, String)}.</li>
 * </ol>
 *
 * <p><strong>Limitation connue</strong> : le secret TOTP est stocké en clair dans la colonne
 * {@code secret_2fa}. Le chiffrement au repos (AES) est reporté à une itération ultérieure.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TwoFactorService {

    /** Nom de l'application affiché dans Google Authenticator. */
    private static final String ISSUER = "LIMS";

    private final GoogleAuthenticator googleAuthenticator;
    private final UtilisateurRepository utilisateurRepository;

    // -------------------------------------------------------------------------
    // Enrôlement — Setup
    // -------------------------------------------------------------------------

    /**
     * Génère un nouveau secret TOTP, le persiste dans {@code secret_2fa} (sans activer la 2FA)
     * et retourne l'URL TOTP pour générer un QR code côté client.
     *
     * @param utilisateur utilisateur demandant l'enrôlement
     * @return URL otpauth:// à encoder en QR code
     */
    @Transactional
    public String setupTotp(Utilisateur utilisateur) {
        if (utilisateur.isDoubleAuthentification()) {
            throw new BusinessRuleException("2FA_ALREADY_ACTIVE",
                    "La double authentification est déjà activée pour ce compte.");
        }
        if (utilisateur.getSecret2fa() != null && !utilisateur.getSecret2fa().isBlank()) {
            return urlTotp(utilisateur.getEmail(), utilisateur.getSecret2fa());
        }
        GoogleAuthenticatorKey credentials = googleAuthenticator.createCredentials();
        String secret = credentials.getKey();

        utilisateur.setSecret2fa(secret);
        utilisateur.setDoubleAuthentification(false);
        utilisateurRepository.save(utilisateur);

        log.debug("Secret TOTP préparé pour userId={} — 2FA non encore activée", utilisateur.getId());
        return urlTotp(utilisateur.getEmail(), secret);
    }

    private static String urlTotp(String email, String secret) {
        String compte = URLEncoder.encode(email, StandardCharsets.UTF_8);
        String emetteur = URLEncoder.encode(ISSUER, StandardCharsets.UTF_8);
        return "otpauth://totp/" + emetteur + ":" + compte + "?secret=" + secret + "&issuer=" + emetteur;
    }

    // -------------------------------------------------------------------------
    // Enrôlement — Activation
    // -------------------------------------------------------------------------

    /**
     * Active définitivement la 2FA pour l'utilisateur après vérification d'un premier code TOTP.
     * {@code double_authentification} est mis à {@code true} seulement si le code est correct.
     *
     * @param utilisateur utilisateur qui active la 2FA
     * @param code        code TOTP à 6 chiffres saisi par l'utilisateur
     * @throws BusinessRuleException si le secret n'a pas encore été configuré ou si le code est invalide
     */
    @Transactional
    public void activerTotp(Utilisateur utilisateur, String code) {
        if (utilisateur.getSecret2fa() == null || utilisateur.getSecret2fa().isBlank()) {
            throw new BusinessRuleException("2FA_NOT_SETUP",
                    "Aucun secret TOTP configuré. Appelez d'abord /2fa/setup.");
        }
        if (!verifierTotp(utilisateur.getSecret2fa(), code)) {
            throw new BusinessRuleException("2FA_CODE_INVALID",
                    "Code TOTP incorrect — vérifiez l'heure de votre application et réessayez.");
        }
        utilisateur.setDoubleAuthentification(true);
        utilisateurRepository.save(utilisateur);
        log.info("2FA activée pour userId={}", utilisateur.getId());
    }

    // -------------------------------------------------------------------------
    // Vérification (login)
    // -------------------------------------------------------------------------

    /**
     * Vérifie un code TOTP à 6 chiffres par rapport à un secret connu.
     *
     * @param secret secret Base32 stocké dans {@code secret_2fa}
     * @param code   code saisi par l'utilisateur (6 chiffres)
     * @return {@code true} si le code est valide dans la fenêtre temporelle courante
     */
    public boolean verifierTotp(String secret, String code) {
        if (secret == null || secret.isBlank()) {
            log.warn("verifierTotp appelé avec un secret null ou vide");
            return false;
        }
        try {
            int codeInt = Integer.parseInt(code);
            return googleAuthenticator.authorize(secret, codeInt);
        } catch (NumberFormatException ex) {
            log.warn("Code TOTP non numérique reçu");
            return false;
        }
    }
}
