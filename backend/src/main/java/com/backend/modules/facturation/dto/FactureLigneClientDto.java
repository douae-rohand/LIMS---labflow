package com.backend.modules.facturation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Ligne d'une facture côté client.
 *
 * <p>La table {@code facture_ligne} ne porte que {@code designation},
 * {@code montant} et la liaison {@code ligne_essai_id} : il n'existe ni
 * quantité ni prix unitaire.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FactureLigneClientDto {

    private Long ligneId;
    private Long ligneEssaiId;
    private String designation;
    private BigDecimal montant;
}
