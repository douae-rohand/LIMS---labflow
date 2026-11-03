package com.backend.modules.produit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProduitDto {
    private Long id;
    private String reference;
    private String nom;
    private String unite;
    private BigDecimal seuilMinimal;
    private String conditionsStockage;
    private Boolean actif;
    private BigDecimal stockCourant;
}
