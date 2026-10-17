package com.backend.modules.plateforme.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysePubliqueDto {
    private Long id;
    private String code;
    private String designation;
    private String description;
    private String domaineCode;
    private String domaineLibelle;
    private BigDecimal tarif;
    /** Durée estimée en minutes. */
    private Integer dureeEstimee;
    private List<ExigencePubliqueDto> exigences;
}
