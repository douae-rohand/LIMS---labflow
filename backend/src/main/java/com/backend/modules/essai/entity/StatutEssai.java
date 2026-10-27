package com.backend.modules.essai.entity;

/**
 * Statut d'une ligne d'essai ({@code ligne_essai.statut}), aligné sur le diagramme.
 * {@link StatutEssaiConverter} relit encore les noms historiques.
 */
public enum StatutEssai {
    ATTENTE_ECHANTILLON,
    EN_COURS,
    TERMINE,
    NON_REALISABLE
}
