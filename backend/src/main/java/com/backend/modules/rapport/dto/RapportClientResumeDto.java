package com.backend.modules.rapport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Résumé d'un rapport visible par le client, pour la liste agrégée
 * {@code GET /api/client/rapports}.
 *
 * <p>Aucune donnée interne n'est exposée : ni signataire, ni limites de
 * référence, ni seuils OOS/OOT, ni identifiant d'utilisateur.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RapportClientResumeDto {

    private Long rapportId;
    private Long laboratoireId;
    private String laboratoireNom;
    private Long demandeId;
    private String demandeReference;
    private String demandeTitre;
    private String type;
    private Short version;
    private Instant dateEmission;

    /** Toujours {@code false} : aucun stockage PDF (MinIO/OpenPDF hors périmètre). */
    private boolean telechargeable;
}
