package com.backend.modules.utilisateur.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Ancien sous-type conservé pour le code métier existant.
 * Le modèle de données ne lui associe plus de table : le rôle est porté par {@link Role}.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Technicien extends Utilisateur {

    private String specialite;
    private String numeroAccreditation;
}
