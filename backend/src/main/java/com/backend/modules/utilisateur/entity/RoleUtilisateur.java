package com.backend.modules.utilisateur.entity;

/**
 * Enumération des rôles disponibles dans le LIMS.
 * Chaque rôle correspond à un sous-type de {@link Utilisateur}.
 */
public enum RoleUtilisateur {

    /** Client externe qui soumet des demandes d'analyses. */
    CLIENT,

    /** Agent d'accueil qui réceptionne les échantillons. */
    ACCUEIL,

    /** Technicien qui réalise les essais. */
    TECHNICIEN,

    /** Responsable de laboratoire qui valide les résultats. */
    RESPONSABLE,

    /** Administrateur d'un laboratoire (tenant). */
    ADMINISTRATEUR,

    /** Super-administrateur de la plateforme (schéma central). */
    SUPER_ADMINISTRATEUR
}
