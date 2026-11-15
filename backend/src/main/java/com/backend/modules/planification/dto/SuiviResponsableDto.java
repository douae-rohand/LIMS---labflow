package com.backend.modules.planification.dto;

import java.util.List;

/** Tableau de suivi du responsable : charge de l'équipe et dossiers en retard. */
public record SuiviResponsableDto(
        Indicateurs indicateurs,
        List<TechnicienChargeDto> techniciens,
        List<LigneTravailDto> retards) {

    public record Indicateurs(
            long demandesADecider,
            long analysesAAffecter,
            long analysesEnCours,
            long analysesEnRetard,
            long resultatsAValider,
            long rapportsAPreparer,
            /** Demandes ayant au moins une analyse sans technicien. */
            long demandesAAffecter) {
    }
}
