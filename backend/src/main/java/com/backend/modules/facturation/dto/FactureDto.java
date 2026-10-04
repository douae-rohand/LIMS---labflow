package com.backend.modules.facturation.dto;

import com.backend.modules.facturation.entity.StatutFacture;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class FactureDto {
    private Long id;
    private String numero;
    private Long demandeId;
    private Long clientId;
    private String laboratoireCode;
    private StatutFacture statut;
    private BigDecimal montantHt;
    private BigDecimal tauxTva;
    private BigDecimal montantTtc;
    private LocalDate dateEmission;
    private LocalDate dateEcheance;
    private Instant datePaiement;
    private String fichierUrl;
}
