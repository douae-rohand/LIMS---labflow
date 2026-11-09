package com.backend.modules.technicien.dto;

import jakarta.validation.constraints.Size;

/**
 * Corps de POST /{id}/validation-technique.
 *
 * <p>Le technicien ne peut que VALIDER (jamais rejeter ni retourner).
 * La décision est donc fixée à "VALIDEE" côté service, jamais fournie par le client.</p>
 *
 * <p>Le commentaire est facultatif.</p>
 */
public record ValidationTechniqueRequest(
        @Size(max = 2000, message = "Le commentaire ne peut pas dépasser 2000 caractères.")
        String commentaire
) {
}
