package com.backend.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DisableTwoFactorRequest(
        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(max = 100, message = "Le mot de passe est invalide")
        String motDePasse,

        @NotBlank(message = "Le code TOTP est obligatoire")
        @Pattern(regexp = "^\\d{6}$", message = "Le code TOTP doit contenir exactement 6 chiffres")
        String code
) {}
