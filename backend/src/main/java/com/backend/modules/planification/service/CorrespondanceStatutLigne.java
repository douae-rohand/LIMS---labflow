package com.backend.modules.planification.service;

import com.backend.modules.essai.entity.StatutEssai;
import com.backend.modules.planification.entity.StatutPlanification;

/**
 * Traduit le vocabulaire de planification vers le statut canonique de {@code ligne_essai}.
 * Les noms de {@link StatutPlanification} restent ceux exposés par l'API.
 */
public final class CorrespondanceStatutLigne {

    private CorrespondanceStatutLigne() {
    }

    public static StatutEssai versLigne(StatutPlanification statut) {
        if (statut == null) {
            return StatutEssai.PLANIFIEE;
        }
        return switch (statut) {
            case PLANIFIEE -> StatutEssai.PLANIFIEE;
            case EN_COURS -> StatutEssai.EN_COURS;
            case TERMINEE -> StatutEssai.TERMINE;
            case ANNULEE -> StatutEssai.ANNULE;
            case REPORTEE -> StatutEssai.REPORTEE;
        };
    }

    public static StatutPlanification versPlanification(StatutEssai statut) {
        if (statut == null) {
            return null;
        }
        return switch (statut) {
            case PLANIFIEE -> StatutPlanification.PLANIFIEE;
            case EN_COURS -> StatutPlanification.EN_COURS;
            case TERMINE -> StatutPlanification.TERMINEE;
            case ANNULE -> StatutPlanification.ANNULEE;
            case REPORTEE -> StatutPlanification.REPORTEE;
            case EN_ATTENTE, A_REFAIRE -> null;
        };
    }
}
