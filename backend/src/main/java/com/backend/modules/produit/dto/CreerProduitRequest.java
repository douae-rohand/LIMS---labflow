package com.backend.modules.produit.dto;

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
public class CreerProduitRequest {
    @NotBlank(message = "La référence est obligatoire")
    @Size(max = 50, message = "La référence ne peut pas dépasser 50 caractères")
    private String reference;

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 255, message = "Le nom ne peut pas dépasser 255 caractères")
    private String nom;

    @NotBlank(message = "L'unité est obligatoire")
    @Size(max = 50, message = "L'unité ne peut pas dépasser 50 caractères")
    private String unite;

    @NotNull(message = "Le seuil minimal est obligatoire")
    private BigDecimal seuilMinimal;

    private String conditionsStockage;
}
