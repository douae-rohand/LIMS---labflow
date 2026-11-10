package com.backend.modules.facturation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FactureAdminDto {
    private Long id;
    private String numero;
    private String type;
    private LocalDate dateFacture;
    private LocalDate dateEcheance;
    /** Total de la facture, TTC (les prix du catalogue sont saisis TTC). */
    private BigDecimal montantHt;
    /** Taux de TVA appliqué (%), visible uniquement dans l'espace Administrateur. */
    private BigDecimal tauxTva;
    /** Part de TVA comprise dans le total TTC. */
    private BigDecimal montantTva;
    /** Total hors taxes : total TTC − TVA. */
    private BigDecimal montantHorsTaxes;
    private String statut;
    private Long demandeId;
    private String demandeReference;
    private String demandeTitre;
    private Long clientId;
    private String clientNom;
    private String clientIce;
    private List<FactureLigneAdminDto> lignes;
    private List<PaiementAdminDto> paiements;
}
