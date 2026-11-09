package com.backend.modules.technicien.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Réponse de POST /api/technicien/essais/import/apercu.
 *
 * <p>Le champ {@code pret} vaut {@code true} uniquement si aucune erreur
 * n'a été détectée : le frontend peut alors proposer la confirmation.</p>
 *
 * <p>En cas d'erreurs, {@code apercu} contient quand même les lignes
 * valides pour aider le technicien à identifier les problèmes.</p>
 */
@Value
@Builder
public class ImportApercuResponse {
    /** Nombre de lignes CSV lues (hors en-tête). */
    int lignesLues;
    /** Nombre de lignes sans erreur. */
    int lignesOk;
    /** Nombre de lignes en erreur. */
    int lignesErreur;
    /** true = toutes les lignes sont valides, la confirmation peut être lancée. */
    boolean pret;
    /** Liste des erreurs par ligne. */
    List<ImportLigneErreurDto> erreurs;
    /** Aperçu des lignes valides (sans écriture). */
    List<ImportApercuLigneDto> apercu;
}
