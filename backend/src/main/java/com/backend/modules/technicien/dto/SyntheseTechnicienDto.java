package com.backend.modules.technicien.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.List;

/**
 * Tableau de bord du technicien.
 * Tous les compteurs proviennent de requêtes SQL d'agrégation :
 * aucun chargement d'entité complet.
 */
@Value
@Builder
public class SyntheseTechnicienDto {

    /** Essais dont la date_attribution est aujourd'hui. */
    long essaisAujourdhuiCount;

    /** Essais en statut EN_COURS. */
    long enCoursCount;

    /** Essais en retard (échéance dépassée, non terminés). */
    long enRetardCount;

    /** Essais en ATTENTE_ECHANTILLON. */
    long enAttenteEchantillonCount;

    /** Essais TERMINÉ sur les 7 derniers jours. */
    long terminesSemaineCount;

    /**
     * Essais EN_COURS avec valeur saisie mais sans validation niveau 1
     * postérieure à la dernière saisie (= en attente de validation technique).
     */
    long validationsTechniquesAttenteCount;

    /**
     * Charge des 7 prochains jours :
     * nombre d'essais attribués par jour (date_attribution).
     */
    List<ChargeSemaineDto> chargeSemaine;

    @Value
    @Builder
    public static class ChargeSemaineDto {
        LocalDate date;
        long count;
    }
}
