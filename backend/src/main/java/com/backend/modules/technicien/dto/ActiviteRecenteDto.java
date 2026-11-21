package com.backend.modules.technicien.dto;

import lombok.Builder;

import java.time.Instant;

/**
 * Un événement récent du journal d'audit lié à un essai du technicien courant.
 */
@Builder
public record ActiviteRecenteDto(
        /** Identifiant de la ligne d'essai concernée. */
        Long ligneEssaiId,
        /** Code de la ligne d'essai (ex. LE-TECH-0005). */
        String codeLigne,
        /** Désignation de l'essai (ex. Glycémie à jeun). */
        String essaiDesignation,
        /** Code de l'essai (ex. GLY). */
        String essaiCode,
        /** Action réalisée : ESSAI_DEMARRE, RESULTAT_SAISI, VALIDATION_TECHNIQUE, ESSAI_NON_REALISABLE, TEMPS_DECLARE. */
        String action,
        /** Date et heure de l'événement. */
        Instant dateAction,
        /** Motif ou commentaire s'il y en a un. */
        String motif
) {}
