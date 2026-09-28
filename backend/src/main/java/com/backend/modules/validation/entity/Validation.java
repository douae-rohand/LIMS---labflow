package com.backend.modules.validation.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/**
 * Validation des résultats d'essais par un responsable (M06).
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "validation")
public class Validation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "essai_id", nullable = false)
    private Long essaiId;

    @Column(name = "validateur_id", nullable = false)
    private Long validateurId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StatutValidation statut = StatutValidation.EN_ATTENTE;

    @Column(name = "commentaire", columnDefinition = "TEXT")
    private String commentaire;

    @Column(name = "date_validation")
    private Instant dateValidation;

    @Column(name = "date_creation", nullable = false, updatable = false)
    @Builder.Default
    private Instant dateCreation = Instant.now();
}
