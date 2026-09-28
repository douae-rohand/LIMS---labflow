package com.backend.modules.plateforme.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Demande d'intégration d'un laboratoire sur la plateforme.
 * Stockée dans le schéma central. Soumise par un représentant du labo,
 * traitée par le super-administrateur.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "demande_integration")
public class DemandeIntegration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nom du laboratoire demandant l'intégration. */
    @Column(name = "nom_laboratoire", nullable = false, length = 200)
    private String nomLaboratoire;

    /** Email du représentant soumettant la demande. */
    @Column(name = "email_representant", nullable = false, length = 180)
    private String emailRepresentant;

    @Column(name = "nom_representant", length = 200)
    private String nomRepresentant;

    @Column(name = "telephone_representant", length = 20)
    private String telephoneRepresentant;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StatutIntegration statut = StatutIntegration.EN_ATTENTE;

    /** Commentaire du super-administrateur lors du traitement. */
    @Column(name = "commentaire_admin", columnDefinition = "TEXT")
    private String commentaireAdmin;

    /** Référence vers le laboratoire créé si la demande est approuvée. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "laboratoire_id")
    private Laboratoire laboratoire;

    @Column(name = "date_soumission", nullable = false, updatable = false)
    @Builder.Default
    private Instant dateSoumission = Instant.now();

    @Column(name = "date_traitement")
    private Instant dateTraitement;
}
