package com.backend.modules.catalogue.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Type d'analyse proposé au catalogue du laboratoire (M04).
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "analyse_type")
public class AnalyseType {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 300)
    private String designation;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Norme ou méthode de référence (ex. ISO 6579, NF EN 14182). */
    @Column(name = "norme_reference", length = 200)
    private String normeReference;

    @Column(name = "delai_jours")
    private Integer delaiJours;

    @Column(name = "prix_unitaire", precision = 12, scale = 2)
    private BigDecimal prixUnitaire;

    @Column(name = "unite_mesure", length = 50)
    private String uniteMesure;

    @Column(name = "est_actif", nullable = false)
    @Builder.Default
    private boolean actif = true;

    @Column(name = "date_creation", nullable = false, updatable = false)
    @Builder.Default
    private Instant dateCreation = Instant.now();
}
