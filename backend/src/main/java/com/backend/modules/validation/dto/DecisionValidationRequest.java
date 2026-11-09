package com.backend.modules.validation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Décision du responsable sur un résultat déjà validé techniquement. */
public record DecisionValidationRequest(
        @NotNull(message = "La décision est obligatoire") Decision decision,
        @Size(max = 2000, message = "Le motif ne peut pas dépasser 2000 caractères") String motif) {

    public enum Decision {
        /** Le résultat est validé et pourra figurer au rapport. */
        VALIDER,
        /** Le technicien doit revoir sa saisie (le résultat est conservé). */
        RETOURNER,
        /** Le résultat est écarté : l'essai est à refaire. */
        REJETER
    }
}
