package com.backend.modules.demande.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Corps de {@code POST /api/client/demandes/{laboratoireId}}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreerDemandeRequest {

    @NotBlank(message = "Le titre de la demande est obligatoire.")
    @Size(max = 255, message = "Le titre ne peut pas dépasser 255 caractères.")
    private String titre;

    private String objectif;
}
