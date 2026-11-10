package com.backend.modules.rapport.dto;

import com.backend.modules.rapport.entity.StatutRapport;

import java.time.Instant;

/** Demande terminée et son rapport : à signer (aucun rapport ou projet) ou signé. */
public record RapportTravailDto(
        Long demandeId,
        String demandeReference,
        String demandeTitre,
        String client,
        long nombreAnalyses,
        Long rapportId,
        String numero,
        StatutRapport statut,
        Instant dateEmission,
        Instant dateSignature,
        String signataire) {
}
