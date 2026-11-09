package com.backend.modules.plateforme.entity;

import com.backend.common.config.SchemaConstants;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Type de laboratoire proposé à l'intégration et dans les paramètres d'un laboratoire.
 * Le libellé est la valeur enregistrée sur les laboratoires : il n'est pas modifiable.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "referentiel_type_laboratoire", schema = SchemaConstants.CENTRAL_SCHEMA)
public class ReferentielTypeLaboratoire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String libelle;

    @Builder.Default
    @Column(nullable = false)
    private boolean actif = true;

    @Builder.Default
    @Column(nullable = false)
    private int ordre = 0;
}
