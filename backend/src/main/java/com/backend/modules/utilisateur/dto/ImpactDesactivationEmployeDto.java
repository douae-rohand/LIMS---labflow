package com.backend.modules.utilisateur.dto;

public record ImpactDesactivationEmployeDto(
        long essaisOuverts,
        long responsablesActifs,
        boolean dernierCompteActifDuRole) {
}
