package com.backend.modules.facturation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaiementAdminDto {
    private Long id;
    private String code;
    private Instant datePaiement;
    private BigDecimal montant;
    private String mode;
    private String reference;
    private String encaisseurNom;
}
