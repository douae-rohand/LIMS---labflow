package com.backend.modules.panier.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Corps de la requête POST /api/client/paniers/{laboratoireId}/essais.
 * Seul essaiId est attendu.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AjouterEssaiRequest {

    @NotNull(message = "L'identifiant de l'essai est obligatoire")
    @Positive(message = "L'identifiant de l'essai doit être positif")
    private Long essaiId;
}
