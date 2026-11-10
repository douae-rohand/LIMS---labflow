package com.backend.modules.planification.dto;

import java.util.List;

/** Charge d'un technicien : analyses à faire, en cours et en retard. */
public record TechnicienChargeDto(
        Long id,
        String nomComplet,
        String email,
        List<String> domaines,
        long aFaire,
        long enCours,
        long enRetard) {

    public long total() {
        return aFaire + enCours;
    }
}
