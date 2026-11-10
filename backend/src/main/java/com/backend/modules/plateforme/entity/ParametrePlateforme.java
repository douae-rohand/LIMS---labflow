package com.backend.modules.plateforme.entity;

import com.backend.common.config.SchemaConstants;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Paramètre global de la plateforme, stocké sous forme clé / valeur. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "parametre_plateforme", schema = SchemaConstants.CENTRAL_SCHEMA)
public class ParametrePlateforme {

    @Id
    @Column(length = 100)
    private String cle;

    @Column(nullable = false, length = 1000)
    private String valeur;

    @Column(name = "date_modification", nullable = false)
    private Instant dateModification;

    @Column(name = "modifie_par", length = 255)
    private String modifiePar;
}
