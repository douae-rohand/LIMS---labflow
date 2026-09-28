package com.backend.modules.demande.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Demande d'analyse déposée par un client (M01).
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "demande")
public class Demande {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference", nullable = false, unique = true, length = 50)
    private String reference;

    @Column(name = "objet", nullable = false, length = 500)
    private String objet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StatutDemande statut = StatutDemande.BROUILLON;

    /** Identifiant de l'utilisateur client (clé logique — pas de FK cross-schema). */
    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "date_soumission")
    private Instant dateSoumission;

    @Column(name = "date_souhaitee")
    private LocalDate dateSouhaitee;

    @Column(name = "commentaire", columnDefinition = "TEXT")
    private String commentaire;

    @Column(name = "date_creation", nullable = false, updatable = false)
    @Builder.Default
    private Instant dateCreation = Instant.now();

    @Column(name = "date_modification")
    private Instant dateModification;

    @PreUpdate
    protected void onUpdate() { this.dateModification = Instant.now(); }
}
