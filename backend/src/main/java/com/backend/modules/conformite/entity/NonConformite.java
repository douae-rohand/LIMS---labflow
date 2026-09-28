package com.backend.modules.conformite.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/**
 * Non-conformité détectée dans le processus qualité (M13).
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "non_conformite")
public class NonConformite {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference", unique = true, nullable = false, length = 50)
    private String reference;

    @Column(name = "titre", nullable = false, length = 300)
    private String titre;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "gravite", nullable = false, length = 20)
    private GraviteNonConformite gravite;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StatutNonConformite statut = StatutNonConformite.OUVERTE;

    /** Entité source (Essai, Echantillon, Demande…). */
    @Column(name = "entite_source", length = 100)
    private String entiteSource;

    @Column(name = "id_entite_source")
    private Long idEntiteSource;

    @Column(name = "declarant_id")
    private Long declarantId;

    @Column(name = "responsable_traitement_id")
    private Long responsableTraitementId;

    @Column(name = "action_corrective", columnDefinition = "TEXT")
    private String actionCorrective;

    @Column(name = "date_detection", nullable = false, updatable = false)
    @Builder.Default
    private Instant dateDetection = Instant.now();

    @Column(name = "date_cloture")
    private Instant dateCloture;
}
