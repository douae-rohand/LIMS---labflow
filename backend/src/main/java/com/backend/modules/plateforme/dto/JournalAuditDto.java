package com.backend.modules.plateforme.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class JournalAuditDto {

    private final Long id;
    private final String code;
    private final Instant dateAction;
    private final String action;
    private final String objet;
    private final String valeurAvant;
    private final String valeurApres;
    private final String motif;
    private final String acteurEmail;
    private final String acteurNom;
}
