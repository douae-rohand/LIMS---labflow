package com.backend.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Corps de la requête {@code POST /api/auth/mot-de-passe/changer}.
 */
@Data
public class ChangerMotDePasseRequest {

    @NotBlank(message = "L'ancien mot de passe est obligatoire")
    private String ancienMotDePasse;

    @NotBlank(message = "Le nouveau mot de passe est obligatoire")
    private String nouveauMotDePasse;
}
