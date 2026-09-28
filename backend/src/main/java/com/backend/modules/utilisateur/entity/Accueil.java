package com.backend.modules.utilisateur.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Agent d'accueil : réceptionne les échantillons et enregistre les demandes.
 * Table : {@code utilisateur_accueil}.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "utilisateur_accueil")
@DiscriminatorValue("ACCUEIL")
public class Accueil extends Utilisateur {

    /** Poste ou emplacement physique de l'agent d'accueil. */
    @Column(name = "poste", length = 100)
    private String poste;
}
