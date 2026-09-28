package com.backend.modules.catalogue.dto;

import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AnalyseTypeDto {
    private Long id;
    private String code;
    private String designation;
    private String description;
    private String normeReference;
    private Integer delaiJours;
    private BigDecimal prixUnitaire;
    private String uniteMesure;
    private boolean actif;
}
