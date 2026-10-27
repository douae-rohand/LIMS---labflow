package com.backend.modules.panier.entity;

import com.backend.common.config.SchemaConstants;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Ligne d'un panier. essaiId est un Long simple (sans FK) car l'essai
 * vit dans le schéma du laboratoire, pas dans lims_central.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ligne_panier", schema = SchemaConstants.CENTRAL_SCHEMA, uniqueConstraints = {
        @UniqueConstraint(name = "uk_ligne_panier_essai", columnNames = {"panier_id", "essai_id"})
})
public class LignePanier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ligne_panier")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "panier_id", nullable = false)
    private Panier panier;

    @Column(name = "essai_id", nullable = false)
    private Long essaiId;

    @Column(name = "date_ajout", nullable = false)
    private Instant dateAjout;
}
