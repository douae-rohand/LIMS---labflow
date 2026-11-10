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
 * Domaine d'analyse de référence. Les domaines actifs sont copiés dans le
 * catalogue de chaque nouveau laboratoire au moment de son provisionnement.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "referentiel_domaine", schema = SchemaConstants.CENTRAL_SCHEMA)
public class ReferentielDomaine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String libelle;

    @Builder.Default
    @Column(nullable = false)
    private boolean actif = true;

    @Builder.Default
    @Column(nullable = false)
    private int ordre = 0;
}
