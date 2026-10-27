package com.backend.modules.demande.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyseDemandeeDto {
    private Long id;
    private String code;
    private String designation;
    private String methode;
    private String unite;
    private BigDecimal tarif;
    private BigDecimal montant;
    private String statut;
}
