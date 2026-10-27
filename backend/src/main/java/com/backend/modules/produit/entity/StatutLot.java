package com.backend.modules.produit.entity;

/**
 * Disponibilité d'un lot. Le diagramme écrit {@code PÉRIMÉ} ; le code persiste
 * {@code PERIME}, comme les autres statuts, sans accent.
 */
public enum StatutLot {
    DISPONIBLE,
    PERIME,
    EPUISE
}
