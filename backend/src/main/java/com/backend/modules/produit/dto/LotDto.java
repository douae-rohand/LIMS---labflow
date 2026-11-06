package com.backend.modules.produit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LotDto {
    private Long id;
    private String numero;
    private BigDecimal quantite;
    private LocalDate peremption;
    private String statut;
    private Long produitId;
    private String produitReference;
}
