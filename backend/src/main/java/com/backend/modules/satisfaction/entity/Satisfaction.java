package com.backend.modules.satisfaction.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/**
 * Évaluation de satisfaction client après livraison d'un rapport (M10).
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "satisfaction")
public class Satisfaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "demande_id", nullable = false)
    private Long demandeId;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    /** Note globale de 1 à 5. */
    @Column(name = "note", nullable = false)
    private Integer note;

    @Column(name = "commentaire", columnDefinition = "TEXT")
    private String commentaire;

    /** Note délai de rendu. */
    @Column(name = "note_delai")
    private Integer noteDelai;

    /** Note qualité du rapport. */
    @Column(name = "note_qualite")
    private Integer noteQualite;

    /** Note communication. */
    @Column(name = "note_communication")
    private Integer noteCommunication;

    @Column(name = "date_reponse", nullable = false, updatable = false)
    @Builder.Default
    private Instant dateReponse = Instant.now();
}
