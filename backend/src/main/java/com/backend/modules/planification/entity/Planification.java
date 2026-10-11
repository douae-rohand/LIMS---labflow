package com.backend.modules.planification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Modèle de planification (M02). Cette classe n'est pas une entité JPA :
 * les créneaux sont persistés sur {@code ligne_essai}. Le statut canonique
 * en base est {@link com.backend.modules.essai.entity.StatutEssai} ;
 * {@link StatutPlanification} reste le vocabulaire exposé par l'API.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Planification {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Référence à la demande (clé logique cross-schema). */
    @Column(name = "demande_id", nullable = false)
    private Long demandeId;

    /** Technicien assigné (clé logique). */
    @Column(name = "technicien_id")
    private Long technicienId;

    @Column(name = "date_debut_prevue")
    private LocalDate dateDebutPrevue;

    @Column(name = "date_fin_prevue")
    private LocalDate dateFinPrevue;

    @Column(name = "date_debut_effective")
    private Instant dateDebutEffective;

    @Column(name = "date_fin_effective")
    private Instant dateFinEffective;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StatutPlanification statut = StatutPlanification.PLANIFIEE;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "date_creation", nullable = false, updatable = false)
    @Builder.Default
    private Instant dateCreation = Instant.now();
}
