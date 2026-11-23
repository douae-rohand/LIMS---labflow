package com.backend.modules.demande.entity;

import java.util.List;

/**
 * Cycle de vie d'une demande :
 * <pre>
 * NOUVEAU → ACCEPTEE → ECHANTILLONS_RECUS → PLANIFIEE → EN_COURS → EN_VALIDATION → TERMINEE
 *        ↘ REFUSEE / ANNULEE
 * </pre>
 * <ul>
 *   <li>ACCEPTEE : acceptée, échantillons attendus (ou reçus en partie, ou non conformes à remplacer).</li>
 *   <li>ECHANTILLONS_RECUS : chaque analyse réalisable a un échantillon conforme (Accueil).</li>
 *   <li>PLANIFIEE : chaque analyse réalisable est affectée à un technicien (Responsable).</li>
 *   <li>EN_COURS : un technicien a démarré la première analyse.</li>
 *   <li>EN_VALIDATION : toutes les analyses sont closes ; validation par le responsable puis rapport.</li>
 *   <li>TERMINEE : rapport signé envoyé au client.</li>
 * </ul>
 */
public enum StatutDemande {
    NOUVEAU, ACCEPTEE, ECHANTILLONS_RECUS, PLANIFIEE, EN_COURS, EN_VALIDATION, TERMINEE, ANNULEE, REFUSEE;

    /** Acceptée, mais aucune analyse démarrée : la réception et l'affectation font avancer la demande. */
    public static final List<StatutDemande> AVANT_DEMARRAGE = List.of(ACCEPTEE, ECHANTILLONS_RECUS, PLANIFIEE);

    /** Demandes en traitement au laboratoire (de l'acceptation à la validation). */
    public static final List<StatutDemande> EN_TRAITEMENT =
            List.of(ACCEPTEE, ECHANTILLONS_RECUS, PLANIFIEE, EN_COURS, EN_VALIDATION);

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
            case "ECHANTILLONS_RECUS" -> ECHANTILLONS_RECUS;
            case "PLANIFIEE" -> PLANIFIEE;
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

    /** Vrai si la demande est acceptée mais qu'aucune analyse n'a encore démarré. */
    public static boolean avantDemarrage(String valeur) {
        return AVANT_DEMARRAGE.contains(depuis(valeur));
    }

    /** Noms persistés d'une liste de statuts (pour les requêtes {@code statut IN :statuts}). */
    public static List<String> noms(List<StatutDemande> statuts) {
        return statuts.stream().flatMap(s -> s.aliasesPersistés().stream()).toList();
    }

    public List<String> aliasesPersistés() {
        return switch (this) {
            case NOUVEAU -> List.of("NOUVEAU", "SOUMISE", "BROUILLON");
            case REFUSEE -> List.of("REFUSEE", "REJETEE");
            default -> List.of(name());
        };
    }
}
