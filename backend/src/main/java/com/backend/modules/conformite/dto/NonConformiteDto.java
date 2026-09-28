package com.backend.modules.conformite.dto;

import com.backend.modules.conformite.entity.GraviteNonConformite;
import com.backend.modules.conformite.entity.StatutNonConformite;
import lombok.*;
import java.time.Instant;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class NonConformiteDto {
    private Long id;
    private String reference;
    private String titre;
    private String description;
    private GraviteNonConformite gravite;
    private StatutNonConformite statut;
    private String entiteSource;
    private Long idEntiteSource;
    private Long declarantId;
    private Long responsableTraitementId;
    private String actionCorrective;
    private Instant dateDetection;
    private Instant dateCloture;
}
