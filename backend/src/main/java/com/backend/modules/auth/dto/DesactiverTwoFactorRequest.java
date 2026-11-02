package com.backend.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record DesactiverTwoFactorRequest(
        @NotBlank(message = "Le code TOTP est obligatoire")
        @Pattern(regexp = "^\\d{6}$", message = "Le code TOTP doit contenir exactement 6 chiffres")
        String code
) {}
