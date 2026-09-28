package com.backend.modules.utilisateur.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Administrateur d'un laboratoire (tenant).
 * Gère les utilisateurs, la configuration et les paramètres du laboratoire.
 * Table : {@code utilisateur_administrateur}.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "utilisateur_administrateur")
@DiscriminatorValue("ADMINISTRATEUR")
public class Administrateur extends Utilisateur {

    /** Périmètre de responsabilité administrative. */
    @Column(name = "perimetre", length = 200)
    private String perimetre;
}
