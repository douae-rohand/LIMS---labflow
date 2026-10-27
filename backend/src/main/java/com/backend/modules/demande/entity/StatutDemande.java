package com.backend.modules.demande.entity;

import java.util.List;

public enum StatutDemande {
    NOUVEAU, ACCEPTEE, EN_COURS, EN_VALIDATION, TERMINEE, ANNULEE, REFUSEE;

    /**
     * Relit les noms déjà persistés ({@code BROUILLON}, {@code SOUMISE}, {@code REJETEE})
     * vers les valeurs du diagramme.
     */
    public static StatutDemande depuis(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }
        return switch (valeur.trim()) {
            case "BROUILLON", "SOUMISE", "NOUVEAU" -> NOUVEAU;
            case "REJETEE", "REFUSEE" -> REFUSEE;
            case "ACCEPTEE" -> ACCEPTEE;
            case "EN_COURS" -> EN_COURS;
            case "EN_VALIDATION" -> EN_VALIDATION;
            case "TERMINEE" -> TERMINEE;
            case "ANNULEE" -> ANNULEE;
            default -> null;
        };
    }

    public static boolean estNouvelle(String valeur) {
        return depuis(valeur) == NOUVEAU;
    }

    public List<String> aliasesPersistés() {
        return switch (this) {
            case NOUVEAU -> List.of("NOUVEAU", "SOUMISE", "BROUILLON");
            case REFUSEE -> List.of("REFUSEE", "REJETEE");
            default -> List.of(name());
        };
    }
}
