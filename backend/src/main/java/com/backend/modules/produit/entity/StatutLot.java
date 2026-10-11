package com.backend.modules.produit.entity;

/**
 * Disponibilité d'un lot. {@code DISPONIBLE} est la valeur déjà enregistrée par le seed.
 * {@code EPUISE} est posé lorsque la quantité atteint zéro.
 */
public enum StatutLot {
    DISPONIBLE,
    EPUISE
}
