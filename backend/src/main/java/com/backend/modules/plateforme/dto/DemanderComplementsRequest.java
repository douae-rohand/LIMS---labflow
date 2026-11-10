package com.backend.modules.plateforme.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Demande de pièces ou de précisions adressée au demandeur d'une intégration. */
public record DemanderComplementsRequest(
        @NotBlank(message = "Le message au demandeur est obligatoire")
        @Size(max = 4000, message = "Le message ne peut pas dépasser 4000 caractères")
        String message,
        List<String> documents) {
}
