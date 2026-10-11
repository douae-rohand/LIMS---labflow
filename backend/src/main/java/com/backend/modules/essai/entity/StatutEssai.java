package com.backend.modules.essai.entity;

/**
 * Statut canonique d'une ligne d'essai ({@code ligne_essai.statut}).
 * <p>
 * Les valeurs déjà persistées ({@code EN_ATTENTE}, {@code EN_COURS}, {@code TERMINE},
 * {@code ANNULE}, {@code A_REFAIRE}) sont conservées. {@code PLANIFIEE} et {@code REPORTEE}
 * couvrent la planification, qui écrit dans la même colonne.
 * Les alias historiques {@code PLANIFIE}, {@code TERMINEE} et {@code ANNULEE}
 * sont relus par {@link StatutEssaiConverter}, puis réécrits sous la forme canonique.
 */
public enum StatutEssai {
    EN_ATTENTE,
    PLANIFIEE,
    EN_COURS,
    TERMINE,
    ANNULE,
    A_REFAIRE,
    REPORTEE
}
