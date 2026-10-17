package com.backend.modules.panier.entity;

import com.backend.common.config.SchemaConstants;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Panier client stocké en base centrale (lims_central).
 * Un panier par couple (utilisateur, laboratoire).
 * Aucune collection de lignes côté entité : les lignes sont lues
 * explicitement par LignePanierRepository, et la suppression d'un panier
 * entraîne la suppression des lignes via la FK ON DELETE CASCADE.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "panier", schema = SchemaConstants.CENTRAL_SCHEMA, uniqueConstraints = {
        @UniqueConstraint(name = "uk_panier_utilisateur_laboratoire", columnNames = {"utilisateur_id", "laboratoire_id"})
})
public class Panier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_panier")
    private Long id;

    @Column(name = "utilisateur_id", nullable = false)
    private Long utilisateurId;

    @Column(name = "laboratoire_id", nullable = false)
    private Long laboratoireId;

    @Column(name = "date_creation", nullable = false)
    private Instant dateCreation;

    @Column(name = "date_modification", nullable = false)
    private Instant dateModification;
}
