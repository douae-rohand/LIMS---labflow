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
public class AnalysePubliqueDto {
    private Long id;
    private String code;
    private String designation;
    private String description;
    private String methode;
    private String domaineCode;
    private String domaineLibelle;
    private BigDecimal tarif;
    private Integer dureeEstimee;
    private String unite;
    private Boolean actif;
}
