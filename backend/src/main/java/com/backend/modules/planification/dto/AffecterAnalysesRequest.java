package com.backend.modules.planification.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/** Affecte une ou plusieurs analyses à un technicien, avec une date de début prévue facultative. */
public record AffecterAnalysesRequest(
        @NotEmpty(message = "Choisissez au moins une analyse") List<Long> ligneIds,
        @NotNull(message = "Choisissez un technicien") Long technicienId,
        LocalDate datePrevue) {
}
