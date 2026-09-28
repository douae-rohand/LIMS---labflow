package com.backend.modules.utilisateur.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Super-administrateur de la plateforme LIMS.
 * Stocké dans le schéma central (pas dans un schéma tenant).
 * Peut créer des laboratoires, gérer les intégrations et accéder à tous les tenants.
 * Table : {@code utilisateur_super_administrateur}.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "utilisateur_super_administrateur")
@DiscriminatorValue("SUPER_ADMINISTRATEUR")
public class SuperAdministrateur extends Utilisateur {

    /** Niveau d'accès étendu (ex. LECTURE, ECRITURE, TOTAL). */
    @Column(name = "niveau_acces", length = 50)
    private String niveauAcces;
}
