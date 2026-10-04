package com.backend.modules.plateforme.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * Événement métier du schéma central. À utiliser uniquement sur la base centrale
 * (colonne {@code laboratoire_id}). Le schéma tenant a sa propre entité
 * {@link com.backend.modules.evenement.entity.EvenementMetierTenant}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "EvenementMetierCentral")
@Table(name = "evenement_metier", uniqueConstraints = {
        @UniqueConstraint(name = "uk_evenement_metier_code", columnNames = "code")
})
public class EvenementMetierCentral {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evenement")
    private Long id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(name = "type", nullable = false, length = 80)
    private String type;

    @Column(name = "date_heure", nullable = false)
    private Instant dateHeure;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "json")
    private String donnees;

    @Builder.Default
    @Column(nullable = false)
    private boolean envoye = false;

    @Builder.Default
    @Column(name = "nb_tentatives", nullable = false)
    private Integer nbTentatives = 0;

    @Column(name = "derniere_erreur", columnDefinition = "TEXT")
    private String derniereErreur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "laboratoire_id")
    private Laboratoire laboratoire;
}
