package com.backend.modules.produit.dto;

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
public class ModifierProduitRequest {
    @Size(max = 255, message = "Le nom ne peut pas dépasser 255 caractères")
    private String nom;

    @Size(max = 50, message = "L'unité ne peut pas dépasser 50 caractères")
    private String unite;

    private BigDecimal seuilMinimal;

    private String conditionsStockage;

    private Boolean actif;
}
