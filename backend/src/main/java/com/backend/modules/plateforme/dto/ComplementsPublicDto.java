package com.backend.modules.plateforme.dto;

import java.time.Instant;
import java.util.List;

/** Vue publique d'une demande de compléments, accessible par le lien envoyé au demandeur. */
public record ComplementsPublicDto(
        String numero,
        String nomLaboratoire,
        String message,
        Instant dateDemande,
        Instant expiration,
        List<DocumentDemande> documents) {

    public record DocumentDemande(String code, String libelle, String raison, boolean dejaFourni) {
    }
}
