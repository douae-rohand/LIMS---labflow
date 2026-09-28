package com.backend.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Corps de la requête de vérification 2FA (TOTP ou code email).
 */
@Data
public class TwoFactorRequest {

    /**
     * Token temporaire reçu lors du premier appel de connexion
     * (quand {@code requiresTwoFactor=true}).
     */
    @NotBlank(message = "Le token temporaire est obligatoire")
    private String twoFactorToken;

    /**
     * Code OTP à 6 chiffres fourni par l'application TOTP ou envoyé par email.
     */
    @NotBlank(message = "Le code OTP est obligatoire")
    @Pattern(regexp = "^\\d{6}$", message = "Le code OTP doit contenir exactement 6 chiffres")
    private String code;
}
