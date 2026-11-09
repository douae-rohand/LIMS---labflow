package com.backend.modules.plateforme.entity;

/**
 * Statut d'une demande d'intégration d'un laboratoire sur la plateforme.
 */
public enum StatutIntegration {
    EN_ATTENTE,
    /** Le Super Administrateur attend des pièces ou des précisions du demandeur. */
    COMPLEMENTS_DEMANDES,
    ACCEPTEE,
    REFUSEE;

    public static StatutIntegration depuis(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }
        return switch (valeur.trim()) {
            case "EN_COURS_VALIDATION", "SUSPENDUE", "EN_ATTENTE" -> EN_ATTENTE;
            case "COMPLEMENTS_DEMANDES" -> COMPLEMENTS_DEMANDES;
            case "APPROUVEE", "ACCEPTEE" -> ACCEPTEE;
            case "REJETEE", "REFUSEE" -> REFUSEE;
            default -> null;
        };
    }
}
