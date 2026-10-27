package com.backend.modules.panier.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Détail d'un panier avec ses lignes (GET /paniers/{laboratoireId}, POST, DELETE).
 * Quand le panier est vide ou supprimé : lignes = [], sousTotal = 0.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PanierDetailDto {

    private Long laboratoireId;
    private String laboratoireNom;
    private List<LignePanierDto> lignes;
    private BigDecimal sousTotal;
    private Instant dateModification;
}
