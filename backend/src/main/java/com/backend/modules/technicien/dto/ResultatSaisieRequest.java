package com.backend.modules.technicien.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corps de PUT /{id}/resultat.
 *
 * <ul>
 *   <li>{@code valeur} : valeur mesurée (obligatoire). Peut être numérique ou texte.</li>
 *   <li>{@code commentaire} : commentaire libre (facultatif).
 *       <strong>OBLIGATOIRE si un résultat est déjà saisi</strong> (motif de modification).
 *       Cette règle est vérifiée côté service (422 MOTIF_MODIFICATION_REQUIS).</li>
 * </ul>
 */
public record ResultatSaisieRequest(
        @NotBlank(message = "La valeur du résultat est obligatoire.")
        @Size(max = 255, message = "La valeur ne peut pas dépasser 255 caractères.")
        String valeur,

        @Size(max = 2000, message = "Le commentaire ne peut pas dépasser 2000 caractères.")
        String commentaire
) {
}
