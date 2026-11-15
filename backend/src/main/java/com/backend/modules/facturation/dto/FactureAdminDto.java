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
    /** Montant de la facture (somme des prix des essais). */
    private BigDecimal montantHt;
    /** Taux de commission de la plateforme (%), figé à l'émission. */
    private BigDecimal tauxCommission;
    /** Commission prélevée par la plateforme sur cette facture. */
    private BigDecimal montantCommission;
    /** Ce qui revient au laboratoire : montant − commission. */
    private BigDecimal montantNetLaboratoire;
    /** Facture annulée : numéro de l'avoir, date et motif. */
    private String numeroAvoir;
    private java.time.Instant dateAnnulation;
    private String motifAnnulation;
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
