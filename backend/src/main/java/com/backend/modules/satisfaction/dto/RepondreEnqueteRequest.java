package com.backend.modules.satisfaction.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Réponse à une enquête de satisfaction. Seule la note globale est obligatoire ;
 * les critères et le commentaire sont facultatifs.
 */
@Data
public class RepondreEnqueteRequest {

    @NotNull
    @Min(1)
    @Max(5)
    private Integer noteGlobale;

    @Min(1)
    @Max(5)
    private Integer noteDelai;

    @Min(1)
    @Max(5)
    private Integer noteClarte;

    @Min(1)
    @Max(5)
    private Integer noteRelation;

    @Size(max = 2000)
    private String commentaire;
}
