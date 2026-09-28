package com.backend.modules.echantillon.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Échantillon reçu et enregistré en laboratoire (M03).
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "echantillon")
public class Echantillon {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code_barre", unique = true, nullable = false, length = 100)
    private String codeBarre;

    @Column(name = "designation", nullable = false, length = 300)
    private String designation;

    @Column(name = "nature", length = 100)
    private String nature;

    /** Référence à la demande associée (clé logique). */
    @Column(name = "demande_id", nullable = false)
    private Long demandeId;

    /** Agent d'accueil ayant réceptionné (clé logique). */
    @Column(name = "receptionnaire_id")
    private Long receptionnaireId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StatutEchantillon statut = StatutEchantillon.EN_ATTENTE_RECEPTION;

    @Column(name = "date_reception")
    private Instant dateReception;

    @Column(name = "date_peremption")
    private LocalDate datePeremption;

    @Column(name = "quantite")
    private Double quantite;

    @Column(name = "unite", length = 20)
    private String unite;

    @Column(name = "conditions_conservation", length = 200)
    private String conditionsConservation;

    @Column(name = "observations", columnDefinition = "TEXT")
    private String observations;

    @Column(name = "date_creation", nullable = false, updatable = false)
    @Builder.Default
    private Instant dateCreation = Instant.now();
}
