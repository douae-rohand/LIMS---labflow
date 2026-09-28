package com.backend.modules.rapport.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/**
 * Rapport d'analyse généré et transmis au client (M07).
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "rapport")
public class Rapport {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference", unique = true, nullable = false, length = 50)
    private String reference;

    @Column(name = "demande_id", nullable = false)
    private Long demandeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StatutRapport statut = StatutRapport.BROUILLON;

    /** Chemin MinIO ou URL du fichier PDF généré. */
    @Column(name = "fichier_url", length = 500)
    private String fichierUrl;

    @Column(name = "generateur_id")
    private Long generateurId;

    @Column(name = "signataire_id")
    private Long signataireId;

    @Column(name = "date_generation")
    private Instant dateGeneration;

    @Column(name = "date_envoi")
    private Instant dateEnvoi;

    @Column(name = "date_creation", nullable = false, updatable = false)
    @Builder.Default
    private Instant dateCreation = Instant.now();
}
