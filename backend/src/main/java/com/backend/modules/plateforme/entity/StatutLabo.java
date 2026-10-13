package com.backend.modules.plateforme.entity;

/**
 * Statut d'un laboratoire. La colonne {@code laboratoire.statut} reste un texte
 * et stocke {@link #name()}.
 */
public enum StatutLabo {
    ACTIF,
    SUSPENDU,
    DESACTIVE
}
