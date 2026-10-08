package com.backend.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Corps de POST /api/auth/mot-de-passe/oublie. */
@Data
public class MotDePasseOublieRequest {

    @NotBlank(message = "L'adresse e-mail est obligatoire.")
    @Email(message = "Format d'e-mail invalide.")
    private String email;
}
