package com.backend.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ActiverCompteRequest {

    @NotBlank(message = "Le jeton d'activation est obligatoire")
    private String token;

    @NotBlank(message = "Le mot de passe est obligatoire")
    private String motDePasse;
}
