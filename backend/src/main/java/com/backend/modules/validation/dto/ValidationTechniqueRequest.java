package com.backend.modules.validation.dto;

import jakarta.validation.constraints.Size;

/** Validation technique (niveau 1) d'un résultat saisi, par le technicien affecté. */
public record ValidationTechniqueRequest(
        @Size(max = 2000, message = "Le commentaire ne peut pas dépasser 2000 caractères") String commentaire) {
}
