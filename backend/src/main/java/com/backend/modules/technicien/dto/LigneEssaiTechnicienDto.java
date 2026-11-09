package com.backend.modules.technicien.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

/**
 * DTO liste/planning : informations d'une ligne d'essai pour le technicien.
 *
 * Champs EXCLUS intentionnellement :
 *   - montant, tarif           (données financières)
 *   - clientId, clientEmail    (données personnelles)
 *   - decideurId               (identifiant interne)
 *   - commentaires internes
 *
 * Les limites de référence (limite_min/max, seuils) NE sont PAS exposées
 * dans le DTO liste. Elles sont uniquement disponibles dans le détail
 * ({@link LigneEssaiDetailTechnicienDto}) pour accompagner la saisie.
 *
 * Règle « enRetard » :
 *   echeanceEstimee = dateAttribution + essai.dureeEstimee (minutes)
 *   enRetard = echeanceEstimee != null
 *              && echeanceEstimee est dans le passé
 *              && statut ∉ {TERMINE, NON_REALISABLE}
 */
@Value
@Builder
public class LigneEssaiTechnicienDto {

    Long ligneEssaiId;
    String code;
    EssaiRefDto essai;

    /** Numéro de la demande parente (ex. DEM-2026-0001). */
    String demandeNumero;
    String demandeTitre;

    /** Null si aucun échantillon n'est encore rattaché. */
    EchantillonRefDto echantillon;

    /** Valeur de {@link com.backend.modules.essai.entity.StatutEssai}. */
    String statut;

    Instant dateAttribution;

    /**
     * Date estimée de fin = dateAttribution + dureeEstimee minutes.
     * Null si dateAttribution est null.
     */
    Instant echeanceEstimee;

    /** Calculé côté serveur selon la règle documentée ci-dessus. */
    boolean enRetard;

    /**
     * Valeur saisie (peut être null si pas encore saisie).
     * Les limites ne sont exposées qu'en vue détail.
     */
    String valeur;

    /**
     * Niveau de conformité calculé serveur : "CONFORME", "NON_CONFORME",
     * "CRITIQUE", "NON_EVALUE".
     * Jamais fourni par le client ; jamais null.
     */
    String niveauConformite;
}
