package com.backend.modules.utilisateur.entity;

/**
 * Statut d'un employé au sein d'un laboratoire.
 *
 * <p>Règles de calcul basées sur {@code actif} et {@code compte_confirme} :
 * <ul>
 *   <li>{@link #ACTIF} : {@code actif == true && compteConfirme == true}</li>
 *   <li>{@link #DESACTIVE} : {@code !actif && compteConfirme == true}</li>
 *   <li>{@link #EN_ATTENTE} : {@code !compteConfirme} (quel que soit {@code actif}).
 *       Note : l'état (actif=true, compteConfirme=false) est atteignable tant que le lot 4
 *       n'a pas interdit d'activer un compte encore en attente.</li>
 * </ul>
 */
public enum StatutEmploye {
    ACTIF,
    EN_ATTENTE,
    DESACTIVE
}
