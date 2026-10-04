package com.backend.modules.evenement.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * Événement métier d'un schéma tenant. Pas de {@code laboratoire_id}.
 * À utiliser quand la connexion pointe vers le schéma du laboratoire,
 * pas vers la base centrale.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "EvenementMetierTenant")
@Table(name = "evenement_metier", uniqueConstraints = {
        @UniqueConstraint(name = "uk_evenement_metier_tenant_code", columnNames = "code")
})
public class EvenementMetierTenant {

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
    private Boolean envoye = false;

    @Builder.Default
    @Column(name = "nb_tentatives")
    private Integer nbTentatives = 0;

    @Column(name = "derniere_erreur", columnDefinition = "TEXT")
    private String derniereErreur;
}
