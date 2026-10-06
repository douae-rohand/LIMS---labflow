package com.backend.modules.plateforme.dto;

import com.backend.modules.plateforme.entity.StatutIntegration;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TraiterDemandeIntegrationRequest {

    @NotNull(message = "La décision est obligatoire")
    private StatutIntegration decision;

    @Size(max = 4000)
    private String motifRefus;
}
