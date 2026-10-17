package com.backend.modules.demande.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Une entrée de l'historique d'une demande (issue de journal_audit).
 * N'expose jamais l'identité de l'acteur.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoriqueEntreeDto {

    private String action;
    private String statutAvant;
    private String statutApres;
    private String motif;
    private Instant dateAction;
    private String acteur;
}
