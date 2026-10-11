package com.backend.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Corps de la requête POST /api/auth/compte/activer/verifier.
 */
@Data
public class VerifierJetonActivationRequest {

    @NotBlank(message = "Le jeton d'activation est obligatoire")
    private String token;
}
