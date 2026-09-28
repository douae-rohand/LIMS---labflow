package com.backend.modules.plateforme.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Représente un laboratoire intégré sur la plateforme LIMS.
 * Stocké dans le schéma central ({@code lims_central}).
 *
 * <p>Chaque laboratoire correspond à un tenant (schéma MySQL {@code lims_<tenantId>}).
 * Le champ {@code schemaName} contient le nom exact du schéma MySQL alloué.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "laboratoire",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_labo_code", columnNames = "code"),
                @UniqueConstraint(name = "uk_labo_schema", columnNames = "schema_name")
        })
public class Laboratoire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Code court unique du laboratoire (utilisé comme tenantId). */
    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 200)
    private String nom;

    @Column(length = 500)
    private String description;

    /** Nom du schéma MySQL alloué (ex. {@code lims_laboparis}). */
    @Column(name = "schema_name", nullable = false, length = 100)
    private String schemaName;

    /** URL publique du laboratoire (optionnel). */
    @Column(name = "url_site", length = 300)
    private String urlSite;

    @Column(name = "adresse", length = 500)
    private String adresse;

    @Column(name = "telephone", length = 20)
    private String telephone;

    @Column(name = "email_contact", length = 180)
    private String emailContact;

    /** Numéro d'accréditation (COFRAC, ISO 17025…). */
    @Column(name = "numero_accreditation", length = 100)
    private String numeroAccreditation;

    @Column(name = "est_actif", nullable = false)
    @Builder.Default
    private boolean actif = true;

    @Column(name = "date_creation", nullable = false, updatable = false)
    @Builder.Default
    private Instant dateCreation = Instant.now();

    @Column(name = "date_modification")
    private Instant dateModification;

    @PreUpdate
    protected void onUpdate() {
        this.dateModification = Instant.now();
    }
}
