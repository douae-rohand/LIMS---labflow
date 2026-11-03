package com.backend.common.audit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEntryDto {
    private String code;
    private Instant dateAction;
    private String action;
    private String objet;
    private String valeurAvant;
    private String valeurApres;
    private String motif;
    private String acteurNom;
    private String acteurRole;
}
