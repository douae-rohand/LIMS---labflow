package com.backend.modules.facturation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Paiement d'une facture côté client. L'encaisseur ({@code encaisseur_id})
 * n'est jamais exposé.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaiementClientDto {

    private Instant date;
    private BigDecimal montant;
    private String mode;
}
