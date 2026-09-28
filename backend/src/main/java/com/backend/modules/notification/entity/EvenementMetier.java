package com.backend.modules.notification.entity;

/**
 * Types d'événements métier pouvant déclencher des notifications.
 */
public enum EvenementMetier {

    // Demandes
    DEMANDE_SOUMISE,
    DEMANDE_ACCEPTEE,
    DEMANDE_REJETEE,
    DEMANDE_TERMINEE,

    // Échantillons
    ECHANTILLON_RECEPTIONNE,
    ECHANTILLON_NON_CONFORME,

    // Essais & Validations
    ESSAI_TERMINE,
    RESULTAT_VALIDE,
    RESULTAT_REJETE,

    // Rapports
    RAPPORT_GENERE,
    RAPPORT_ENVOYE,

    // Facturation
    FACTURE_EMISE,
    FACTURE_EN_RETARD,

    // Stock
    STOCK_RUPTURE_IMMINENTE,

    // Non-conformités
    NON_CONFORMITE_DECLAREE,
    NON_CONFORMITE_CLOTUREE,

    // Plateforme
    LABORATOIRE_CREE,
    DEMANDE_INTEGRATION_TRAITEE,

    // Utilisateurs
    COMPTE_CREE,
    COMPTE_DESACTIVE
}
