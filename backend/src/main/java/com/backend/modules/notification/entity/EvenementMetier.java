package com.backend.modules.notification.entity;

/**
 * Types d'événements métier (diagramme : {@code TypeEvenement}).
 */
public enum EvenementMetier {
    NOUVELLE_DEMANDE,
    DEMANDE_ACCEPTEE,
    DEMANDE_REFUSEE,
    ESSAI_ATTRIBUE,
    ECHANTILLON_RECU,
    RESULTAT_EN_ATTENTE,
    RAPPORT_VALIDE,
    FACTURE_ECHUE,
    DEMANDE_CLOTUREE,
    ALERTE_STOCK
}
