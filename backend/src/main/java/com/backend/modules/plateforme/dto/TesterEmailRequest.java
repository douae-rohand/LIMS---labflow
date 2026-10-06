package com.backend.modules.plateforme.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TesterEmailRequest {

    @NotBlank(message = "L'e-mail de test est obligatoire")
    @Email(message = "E-mail de test invalide")
    private String destinataire;
}
