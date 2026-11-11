package com.backend.modules.planification.dto;

import com.backend.modules.essai.entity.StatutEssai;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Analyse (ligne d'essai) vue par le responsable : planification, validation, retards. */
@Data
@Builder
public class LigneTravailDto {
    private Long id;
    private String code;
    private Long demandeId;
    private String demandeReference;
    private String demandeTitre;
    private String demandeStatut;
    private Instant demandeDateSoumission;
    private String essaiCode;
    private String essaiLibelle;
    private String methode;
    private String unite;
    private BigDecimal limiteMin;
    private BigDecimal limiteMax;
    private Integer dureeEstimeeMinutes;
    private String domaineCode;
    private String domaineLibelle;
    private Long technicienId;
    private String technicienNom;
    private StatutEssai statut;
    private Instant dateAttribution;
    private LocalDate datePrevue;
    private Instant echeance;
    private boolean enRetard;
    private long joursRetard;
    private String valeur;
    private Boolean conformite;
    /** Valeur numérique hors des seuils critiques de l'essai : à traiter en priorité. */
    private boolean critique;
    private Instant dateSaisie;
    private String observations;
    /** NON_SOUMIS, TECHNIQUE_VALIDEE, VALIDEE, RETOURNEE, REJETEE. */
    private String etatValidation;
    private String derniereDecisionMotif;
    private Instant derniereDecisionDate;
    private String derniereDecisionPar;
}
