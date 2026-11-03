package com.backend.modules.auth.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Réponse retournée après une authentification réussie.
 *
 * <p>Si la 2FA est activée, {@code requiresTwoFactor=true} et les tokens
 * sont absents — le client doit appeler {@code POST /api/auth/2fa/valider}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    /** Token JWT d'accès, absent tant que la 2FA n'est pas validée. */
    private String accessToken;

    @Builder.Default
    private String tokenType = "Bearer";

    /** Durée de vie du token d'accès en secondes. */
    private long expiresIn;

    @Builder.Default
    private boolean requiresTwoFactor = false;

    @Builder.Default
    private boolean setupRequired = false;

    /** Jeton temporaire quand {@code requiresTwoFactor} est vrai. */
    private String twoFactorToken;

    // Informations basiques sur l'utilisateur
    private Long userId;
    private String email;
    private String nomComplet;
    private String role;
    private String tenantId;
    private boolean mustChangePassword;

    /** Codes effectifs, sans préfixe. Vide tant que la session n'est pas ouverte. */
    private List<String> permissions;
}
