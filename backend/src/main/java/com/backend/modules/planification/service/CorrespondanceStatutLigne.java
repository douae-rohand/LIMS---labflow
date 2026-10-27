package com.backend.modules.planification.service;

import com.backend.modules.essai.entity.StatutEssai;
import com.backend.modules.planification.entity.StatutPlanification;

/**
 * Traduit le vocabulaire de planification vers le statut de {@code ligne_essai}.
 * Les noms de {@link StatutPlanification} restent ceux exposés par l'API.
 * Plusieurs statuts de planification partagent {@link StatutEssai#ATTENTE_ECHANTILLON}.
 */
public final class CorrespondanceStatutLigne {

    private CorrespondanceStatutLigne() {
    }

    public static StatutEssai versLigne(StatutPlanification statut) {
        if (statut == null) {
            return StatutEssai.ATTENTE_ECHANTILLON;
        }
        return switch (statut) {
            case PLANIFIEE, REPORTEE -> StatutEssai.ATTENTE_ECHANTILLON;
            case EN_COURS -> StatutEssai.EN_COURS;
            case TERMINEE -> StatutEssai.TERMINE;
            case ANNULEE -> StatutEssai.NON_REALISABLE;
        };
    }

    public static StatutPlanification versPlanification(StatutEssai statut) {
        if (statut == null) {
            return null;
        }
        return switch (statut) {
            case ATTENTE_ECHANTILLON -> StatutPlanification.PLANIFIEE;
            case EN_COURS -> StatutPlanification.EN_COURS;
            case TERMINE -> StatutPlanification.TERMINEE;
            case NON_REALISABLE -> StatutPlanification.ANNULEE;
        };
    }
}
