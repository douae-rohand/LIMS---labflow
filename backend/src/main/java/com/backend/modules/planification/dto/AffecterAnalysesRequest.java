package com.backend.modules.planification.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * Affecte une ou plusieurs analyses à un technicien, avec une date de début prévue facultative.
 * Le motif est obligatoire pour réaffecter une analyse déjà confiée à un autre technicien.
 */
public record AffecterAnalysesRequest(
        @NotEmpty(message = "Choisissez au moins une analyse") List<Long> ligneIds,
        @NotNull(message = "Choisissez un technicien") Long technicienId,
        LocalDate datePrevue,
        @Size(max = 500, message = "Le motif ne peut pas dépasser 500 caractères") String motif) {
}
