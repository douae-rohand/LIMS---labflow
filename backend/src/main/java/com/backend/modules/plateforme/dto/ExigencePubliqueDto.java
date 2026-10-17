package com.backend.modules.plateforme.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExigencePubliqueDto {
    private String nature;
    private String etatConservation;
    private BigDecimal quantiteMinimale;
    private String unite;
}
