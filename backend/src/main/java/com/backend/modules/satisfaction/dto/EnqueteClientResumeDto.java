package com.backend.modules.satisfaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Résumé d'une enquête de satisfaction pour la liste agrégée
 * {@code GET /api/client/enquetes}.
 *
 * <p>Aucune colonne booléenne « répondue » n'existe : {@code repondue} est
 * dérivé de {@code date_reponse IS NOT NULL}. {@code noteGlobale} n'est
 * renseignée que si l'enquête a été répondue.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnqueteClientResumeDto {

    private Long enqueteId;
    private Long laboratoireId;
    private String laboratoireNom;
    private String demandeReference;
    private String demandeTitre;
    private Instant dateEnvoi;
    private boolean repondue;
    private Instant dateReponse;
    private Integer noteGlobale;
}
