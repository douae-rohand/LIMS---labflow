package com.backend.modules.essai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/**
 * Essai réalisé sur un échantillon pour un type d'analyse (M05).
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "essai")
public class Essai {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "echantillon_id", nullable = false)
    private Long echantillonId;

    @Column(name = "analyse_type_code", nullable = false, length = 50)
    private String analyseTypeCode;

    @Column(name = "technicien_id")
    private Long technicienId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StatutEssai statut = StatutEssai.EN_ATTENTE;

    @Column(name = "resultat_valeur", length = 200)
    private String resultatValeur;

    @Column(name = "resultat_unite", length = 50)
    private String resultatUnite;

    @Column(name = "valeur_min")
    private Double valeurMin;

    @Column(name = "valeur_max")
    private Double valeurMax;

    @Column(name = "conforme")
    private Boolean conforme;

    @Column(name = "observations", columnDefinition = "TEXT")
    private String observations;

    @Column(name = "date_debut")
    private Instant dateDebut;

    @Column(name = "date_fin")
    private Instant dateFin;

    @Column(name = "date_creation", nullable = false, updatable = false)
    @Builder.Default
    private Instant dateCreation = Instant.now();
}
