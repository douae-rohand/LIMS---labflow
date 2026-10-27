package com.backend.modules.catalogue.dto;

import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AnalyseTypeDto {
    private Long id;
    private String code;
    private String designation;
    private String description;
    /** Méthode ou norme de référence ({@code essai.methode}). */
    private String normeReference;
    /** Durée estimée en minutes. */
    private Integer dureeEstimee;
    /**
     * Alias de {@link #dureeEstimee}, conservé pour les clients existants.
     * La valeur est en minutes, pas en jours.
     */
    private Integer delaiJours;
    private BigDecimal prixUnitaire;
    private String uniteMesure;
    private String domaineCode;
    private String domaineLibelle;
    private boolean actif;
}
