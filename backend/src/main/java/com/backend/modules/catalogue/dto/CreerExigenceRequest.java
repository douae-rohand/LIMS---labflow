package com.backend.modules.catalogue.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreerExigenceRequest {
    @NotBlank(message = "La nature est obligatoire")
    @Size(max = 100, message = "La nature ne peut pas dépasser 100 caractères")
    private String nature;

    @NotBlank(message = "L'état de conservation est obligatoire")
    @Size(max = 255, message = "L'état de conservation ne peut pas dépasser 255 caractères")
    private String etatConservation;

    private BigDecimal quantiteMinimale;

    private String unite;
}
