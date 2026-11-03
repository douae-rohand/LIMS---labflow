package com.backend.modules.rapport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Détail d'un rapport côté client : métadonnées + lignes + synthèse IA.
 *
 * <p>La synthèse IA est un texte brut accompagné de la mention explicite
 * {@code mentionSyntheseIa} ; elle est informative et non contractuelle.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RapportClientDetailDto {

    private Long rapportId;
    private Long laboratoireId;
    private String laboratoireNom;
    private Long demandeId;
    private String demandeReference;
    private String demandeTitre;
    private String type;
    private Short version;
    private Instant dateEmission;
    private Instant dateSignature;

    /** Toujours {@code false} : aucun stockage PDF (MinIO/OpenPDF hors périmètre). */
    private boolean telechargeable;

    private List<RapportLigneClientDto> lignes;

    /** Texte de la synthèse IA, ou {@code null} si absente. */
    private String syntheseIa;

    /** Mention associée à la synthèse IA, ou {@code null} si absente. */
    private String mentionSyntheseIa;
}
