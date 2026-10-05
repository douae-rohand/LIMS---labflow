package com.backend.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Corps de la requête pour {@code POST /api/auth/2fa/setup}.
 *
 * <p>Peut contenir un {@code twoFactorToken} si l'utilisateur est en cours
 * d'enrôlement 2FA obligatoire lors de la connexion.
 */
public record SetupTwoFactorRequest(
        String twoFactorToken
) {}
