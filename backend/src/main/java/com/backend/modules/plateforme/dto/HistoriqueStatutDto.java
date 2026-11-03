package com.backend.modules.plateforme.dto;

import java.time.Instant;

public record HistoriqueStatutDto(
        Long id,
        String ancienStatut,
        String nouveauStatut,
        String motif,
        String acteurEmail,
        Instant dateChangement
) {
}
