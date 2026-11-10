package com.backend.modules.technicien.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corps de POST /{id}/non-realisable.
 * Le motif est obligatoire (règle métier).
 */
public record NonRealisableRequest(
        @NotBlank(message = "Le motif est obligatoire pour déclarer un essai non réalisable.")
        @Size(max = 1000, message = "Le motif ne peut pas dépasser 1000 caractères.")
        String motif
) {
}
