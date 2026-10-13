package com.backend.modules.facturation.entity;

public enum StatutFacture {
    EMISE, PAYEE, EN_RETARD, IMPAYEE;

    public static StatutFacture depuis(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }
        return switch (valeur.trim()) {
            case "ENVOYEE", "EMISE" -> EMISE;
            case "BROUILLON", "ANNULEE", "IMPAYEE" -> IMPAYEE;
            case "PAYEE" -> PAYEE;
            case "EN_RETARD" -> EN_RETARD;
            default -> null;
        };
    }
}
