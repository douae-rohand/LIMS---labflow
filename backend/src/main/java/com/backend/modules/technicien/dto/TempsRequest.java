package com.backend.modules.technicien.dto;

import jakarta.validation.constraints.Positive;

import java.time.Instant;

/**
 * Corps de POST /{id}/temps.
 *
 * <p>Deux formes acceptées (validées côté service) :</p>
 * <ul>
 *   <li>{@code debut} + {@code fin} : la durée est calculée automatiquement (fin &gt; debut).</li>
 *   <li>{@code dureeMinutes} seul : durée déclarée directement.</li>
 * </ul>
 *
 * <p>Une seule valeur {@code duree_minutes} est stockée par ligne d'essai.
 * La dernière déclaration remplace la précédente (avec audit de la valeur précédente).</p>
 *
 * <p>Le plafond est configurable via {@code lims.technicien.duree-max-minutes}
 * (défaut : 480 minutes = 8 heures).</p>
 */
public record TempsRequest(
        Instant debut,
        Instant fin,
        @Positive(message = "La durée en minutes doit être positive.")
        Integer dureeMinutes
) {
}
