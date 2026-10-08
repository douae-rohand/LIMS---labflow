package com.backend.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Corps de la demande de renvoi du lien d'activation du compte (CLI-02).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RenvoyerActivationRequest {

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    @Size(max = 180, message = "L'email ne peut pas dépasser 180 caractères")
    private String email;
}
