package com.backend.modules.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Réponse retournée après une authentification réussie.
 *
 * <p>Si la 2FA est activée, {@code requiresTwoFactor=true} et les tokens
 * sont absents — le client doit appeler {@code POST /api/auth/2fa/verify}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    /** Token JWT d'accès (null si 2FA requise). */
    private String accessToken;

    /** Refresh token (null si 2FA requise). */
    private String refreshToken;

    @Builder.Default
    private String tokenType = "Bearer";

    /** Durée de vie du token d'accès en secondes. */
    private long expiresIn;

    /** Indique si une vérification 2FA est requise pour compléter la connexion. */
    @Builder.Default
    private boolean requiresTwoFactor = false;

    /** Indique si le 2FA est obligatoire mais non encore configuré (setup requis). */
    @Builder.Default
    private boolean setupRequired = false;

    /** Token temporaire renvoyé quand requiresTwoFactor=true (valable quelques minutes). */
    private String twoFactorToken;

    // Informations basiques sur l'utilisateur
    private Long userId;
    private String email;
    private String nomComplet;
    private String role;
    private String tenantId;
    private boolean mustChangePassword;
}
