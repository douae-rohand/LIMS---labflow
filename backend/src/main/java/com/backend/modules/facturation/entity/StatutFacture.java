package com.backend.modules.facturation.entity;

public enum StatutFacture {
    EMISE, PAYEE, EN_RETARD, IMPAYEE,
    /** Annulée par un avoir : plus rien n'est dû, la facture reste conservée et numérotée. */
    ANNULEE;

    public static StatutFacture depuis(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }
        return switch (valeur.trim()) {
            case "ENVOYEE", "EMISE" -> EMISE;
            case "BROUILLON", "IMPAYEE" -> IMPAYEE;
            case "ANNULEE" -> ANNULEE;
            case "PAYEE" -> PAYEE;
            case "EN_RETARD" -> EN_RETARD;
            default -> null;
        };
    }
}
