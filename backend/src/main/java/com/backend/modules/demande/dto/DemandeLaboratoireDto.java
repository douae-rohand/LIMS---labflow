package com.backend.modules.demande.dto;

import com.backend.modules.demande.entity.StatutDemande;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemandeLaboratoireDto {
    private Long id;
    private String reference;
    private String objet;
    private String objectif;
    private StatutDemande statut;
    private Instant dateSoumission;
    private Instant dateDecision;
    private String motif;
    private Long decideurId;
    private String decideurNom;
    private boolean decisionPossible;
    private ClientDemandeDto client;
    private PatientDemandeDto patient;
    private List<AnalyseDemandeeDto> analyses;
    private List<EchantillonDemandeDto> echantillons;
    private List<DocumentDemandeDto> documents;
}
