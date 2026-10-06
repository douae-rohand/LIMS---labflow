package com.backend.modules.plateforme.entity;

import com.backend.common.config.SchemaConstants;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "laboratoire", schema = SchemaConstants.CENTRAL_SCHEMA, uniqueConstraints = {
        @UniqueConstraint(name = "uk_laboratoire_code", columnNames = "code"),
        @UniqueConstraint(name = "uk_laboratoire_nom_schema", columnNames = "nom_schema")
})
public class Laboratoire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_laboratoire")
    private Long id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(name = "raison_sociale", nullable = false, length = 255)
    private String raisonSociale;

    @Column(length = 50)
    private String ice;

    @Column(name = "type_laboratoire", length = 500)
    private String typeLaboratoire;

    @Column(columnDefinition = "TEXT")
    private String adresse;

    @Column(length = 100)
    private String ville;

    @Column(length = 100)
    private String region;

    @Column(length = 100)
    private String pays;

    @Column(name = "code_postal", length = 20)
    private String codePostal;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(length = 50)
    private String telephone;

    @Column(length = 255)
    private String email;

    @Column(name = "site_web", length = 255)
    private String siteWeb;

    @Column(name = "nom_schema", nullable = false, length = 64)
    private String nomSchema;

    @Builder.Default
    @Column(nullable = false, length = 50)
    private String statut = "ACTIF";

    @Builder.Default
    @Column(name = "date_creation", nullable = false, updatable = false)
    private Instant dateCreation = Instant.now();
}
