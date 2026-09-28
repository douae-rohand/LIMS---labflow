package com.backend.modules.stock.dto;

import com.backend.modules.stock.entity.CategorieStock;
import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ArticleStockDto {
    private Long id;
    private String reference;
    private String designation;
    private CategorieStock categorie;
    private Double quantiteDisponible;
    private Double quantiteMinimum;
    private String unite;
    private BigDecimal prixUnitaire;
    private String fournisseur;
    private boolean actif;
    private boolean enRuptureImminente;
}
