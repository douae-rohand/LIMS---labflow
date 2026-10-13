package com.backend.modules.demande.dto;

import com.backend.modules.demande.entity.StatutDemande;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemandeResumeDto {
    private Long id;
    private String reference;
    private String objet;
    private StatutDemande statut;
    private String demandeur;
    private Instant dateSoumission;
    private Instant dateDecision;
    private boolean decisionPossible;
}
