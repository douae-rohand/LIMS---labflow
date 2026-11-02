package com.backend.modules.satisfaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Détail d'une enquête de satisfaction côté client : critères (délai, clarté
 * du rapport, relation client) et réponses si l'enquête a déjà été répondue.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnqueteClientDetailDto {

    private Long enqueteId;
    private Long laboratoireId;
    private String laboratoireNom;
    private Long demandeId;
    private String demandeReference;
    private String demandeTitre;
    private Instant dateEnvoi;
    private boolean repondue;
    private Instant dateReponse;
    private Integer noteGlobale;
    private Integer noteDelai;
    private Integer noteClarte;
    private Integer noteRelation;
    private String commentaire;
}
