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
    private BigDecimal montantHt;
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
