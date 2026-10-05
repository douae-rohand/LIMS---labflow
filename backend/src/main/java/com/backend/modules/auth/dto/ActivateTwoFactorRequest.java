package com.backend.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Corps de la requête de l'endpoint {@code POST /api/auth/2fa/activer}.
 *
 * <p>Contient le code TOTP à 6 chiffres et optionnellement le {@code twoFactorToken}
 * pour l'enrôlement obligatoire au login.
 */
public record ActivateTwoFactorRequest(
        @NotBlank(message = "Le code TOTP est obligatoire")
        @Pattern(regexp = "^\\d{6}$", message = "Le code TOTP doit contenir exactement 6 chiffres")
        String code,

        String twoFactorToken
) {}
