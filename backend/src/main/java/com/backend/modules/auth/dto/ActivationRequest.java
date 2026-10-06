package com.backend.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Corps de POST /api/auth/activation/confirmer. */
@Data
public class ActivationRequest {

    @NotBlank(message = "Le jeton d'activation est obligatoire.")
    private String token;
}
