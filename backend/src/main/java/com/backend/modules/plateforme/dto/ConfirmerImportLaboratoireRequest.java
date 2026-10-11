package com.backend.modules.plateforme.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ConfirmerImportLaboratoireRequest {

    @NotEmpty(message = "Aucune ligne à traiter")
    @Size(max = 500, message = "500 lignes au maximum par importation")
    private List<LigneImportLaboratoireDto> lignes = new ArrayList<>();
}
