package com.backend.modules.plateforme.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RapportImportLaboratoireDto {
    private int total;
    private int valides;
    private int invalides;
    private int enregistrees;
    /** Vrai tant que les lignes n'ont pas été enregistrées. */
    private boolean apercu;
    private String message;
    @Builder.Default
    private List<ResultatLigneImportDto> lignes = new ArrayList<>();
}
