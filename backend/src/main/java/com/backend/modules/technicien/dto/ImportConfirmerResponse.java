package com.backend.modules.technicien.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Réponse de POST /api/technicien/essais/import/confirmer.
 *
 * <p>En cas de succès : {@code lignesImportees} lignes enregistrées, {@code erreurs} vide.</p>
 * <p>En cas d'échec (une ligne invalide) : rollback complet, {@code lignesImportees} = 0,
 * {@code erreurs} non vide.</p>
 */
@Value
@Builder
public class ImportConfirmerResponse {
    int lignesImportees;
    List<ImportLigneErreurDto> erreurs;
}
