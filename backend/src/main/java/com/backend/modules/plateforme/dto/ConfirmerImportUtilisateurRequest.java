package com.backend.modules.plateforme.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ConfirmerImportUtilisateurRequest {

    @NotBlank(message = "L'analyse à confirmer est obligatoire")
    private String jetonAnalyse;
}
