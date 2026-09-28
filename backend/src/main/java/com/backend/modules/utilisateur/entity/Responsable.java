package com.backend.modules.utilisateur.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Responsable de laboratoire : supervise et valide les résultats d'essais.
 * Table : {@code utilisateur_responsable}.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "utilisateur_responsable")
@DiscriminatorValue("RESPONSABLE")
public class Responsable extends Utilisateur {

    /** Service ou département dont le responsable a la charge. */
    @Column(name = "service", length = 150)
    private String service;

    /** Indique si ce responsable est le responsable qualité du labo. */
    @Column(name = "est_responsable_qualite")
    private boolean responsableQualite;
}
