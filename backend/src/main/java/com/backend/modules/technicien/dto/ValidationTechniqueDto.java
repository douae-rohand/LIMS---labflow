package com.backend.modules.technicien.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

/** Entrée de l'historique de validation technique (niveau 1) visible par le technicien. */
@Value
@Builder
public class ValidationTechniqueDto {
    Long id;
    /** Niveau : 1 = technique, 2 = responsable. */
    Integer niveau;
    /** Décision : "VALIDEE", "REJETEE", etc. */
    String decision;
    String motif;
    Instant dateValidation;
}
