package com.backend.modules.technicien.dto;

import lombok.Builder;
import lombok.Value;

/**
 * Aperçu d'une ligne CSV valide : affichée dans la réponse /import/apercu
 * pour permettre une confirmation visuelle avant import définitif.
 */
@Value
@Builder
public class ImportApercuLigneDto {
    int numeroLigne;
    /** Code de la ligne d'essai ciblée. */
    String codeLigne;
    /** Valeur qui sera saisie. */
    String valeur;
    /** Commentaire éventuel. */
    String commentaire;
    /** Niveau de conformité calculé serveur (sans écriture). */
    String niveauConformite;
}
