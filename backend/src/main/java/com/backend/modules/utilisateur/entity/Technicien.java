package com.backend.modules.utilisateur.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Technicien de laboratoire : réalise les essais sur les échantillons.
 * Table : {@code utilisateur_technicien}.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "utilisateur_technicien")
@DiscriminatorValue("TECHNICIEN")
public class Technicien extends Utilisateur {

    /** Spécialité ou domaine de compétence (ex. microbiologie, chimie). */
    @Column(name = "specialite", length = 150)
    private String specialite;

    /** Numéro d'accréditation ou de certification professionnelle. */
    @Column(name = "numero_accreditation", length = 100)
    private String numeroAccreditation;
}
