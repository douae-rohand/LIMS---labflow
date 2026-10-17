package com.backend.modules.panier.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Résumé d'un panier (pour GET /api/client/paniers).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PanierResumeDto {

    private Long laboratoireId;
    private String laboratoireNom;
    private int nombreEssais;
    private BigDecimal sousTotal;
    private Instant dateModification;
}
