package com.backend.modules.planification.dto;

import java.util.List;

/** Tableau de suivi du responsable : charge de l'équipe et dossiers en retard. */
public record SuiviResponsableDto(
        Indicateurs indicateurs,
        List<TechnicienChargeDto> techniciens,
        List<LigneTravailDto> retards,
        /** Résultats à valider avec un score d'anomalie ≥ 40, du plus haut au plus bas (10 au plus). */
        List<LigneTravailDto> alertesAnomalies) {

    public record Indicateurs(
            long demandesADecider,
            long analysesAAffecter,
            long analysesEnCours,
            long analysesEnRetard,
            long resultatsAValider,
            long rapportsAPreparer,
            long analysesAReaffecter,
            /** Demandes ayant au moins une analyse sans technicien. */
            long demandesAAffecter,
            /** Résultats à valider en alerte d'anomalie (score ≥ 40). */
            long resultatsEnAlerte) {
    }
}
