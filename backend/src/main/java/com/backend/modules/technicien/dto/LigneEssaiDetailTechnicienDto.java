package com.backend.modules.technicien.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * DTO détail d'une ligne d'essai : contient en plus les exigences,
 * l'historique de validation et les informations de saisie (limites incluses).
 *
 * Les limites sont exposées ICI car nécessaires pour la saisie.
 * Elles ne figurent pas dans le DTO liste (LigneEssaiTechnicienDto).
 *
 * ❌ Toujours exclus : montant, tarif, clientId, decideurId.
 */
@Value
@Builder
public class LigneEssaiDetailTechnicienDto {

    Long ligneEssaiId;
    String code;
    EssaiRefDto essai;

    String demandeNumero;
    String demandeTitre;

    EchantillonRefDto echantillon;

    String statut;
    Instant dateAttribution;
    Instant dateDebut;
    Instant dateFin;
    Instant dateSaisie;
    Instant echeanceEstimee;
    boolean enRetard;

    String valeur;
    String niveauConformite;

    /** Temps déclaré en minutes (une seule valeur, la dernière déclarée). */
    Integer dureeMinutes;

    // Limites de référence (pour accompagner la saisie)
    BigDecimal limiteMin;
    BigDecimal limiteMax;
    BigDecimal seuilCritiqueMin;
    BigDecimal seuilCritiqueMax;

    /** Exigences d'échantillon définies dans le catalogue pour cet essai. */
    List<ExigenceEchantillonRefDto> exigencesEchantillon;

    /**
     * Historique de toutes les validations (tous niveaux).
     * La validation technique courante = la plus récente postérieure
     * à la dernière modification du résultat (dateSaisie).
     */
    List<ValidationTechniqueDto> historiqueValidations;
}
