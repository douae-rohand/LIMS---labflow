package com.backend.modules.rapport.dto;

import com.backend.modules.rapport.entity.StatutRapport;

import java.time.Instant;

/** Demande terminée et son dernier rapport (absent, projet ou signé). */
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
        String signataire,
        /** Empreinte SHA-256 du PDF signé. */
        String empreinte,
        /** Page publique de vérification (rapport libéré avec PDF signé). */
        String urlVerification) {
}
