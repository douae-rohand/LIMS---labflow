package com.backend.modules.technicien.dto;

import lombok.Builder;
import lombok.Value;

/** Erreur de validation sur une ligne du fichier CSV d'import. */
@Value
@Builder
public class ImportLigneErreurDto {
    /** Numéro de ligne dans le fichier (1-indexé, en-tête = 1). */
    int numeroLigne;
    /** Nom de la colonne concernée (code_ligne, valeur, commentaire). */
    String colonne;
    /** Code d'erreur métier. */
    String code;
    /** Description lisible de l'erreur. */
    String message;
}
