package com.backend.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Corps de POST /api/auth/mot-de-passe/reinitialiser. */
@Data
public class ReinitialiserMotDePasseRequest {

    @NotBlank(message = "Le jeton de réinitialisation est obligatoire.")
    private String token;

    @NotBlank(message = "Le nouveau mot de passe est obligatoire.")
    @Size(min = 10, message = "Le mot de passe doit contenir au moins 10 caractères.")
    @Pattern(regexp = ".*[a-z].*", message = "Le mot de passe doit contenir au moins une lettre minuscule.")
    @Pattern(regexp = ".*[A-Z].*", message = "Le mot de passe doit contenir au moins une lettre majuscule.")
    @Pattern(regexp = ".*[0-9].*", message = "Le mot de passe doit contenir au moins un chiffre.")
    private String nouveauMotDePasse;
}
