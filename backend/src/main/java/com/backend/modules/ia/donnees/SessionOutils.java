package com.backend.modules.ia.donnees;

import com.backend.common.tenant.TenantExecutor;
import com.backend.modules.ia.dto.AssistantDtos.Source;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * État d'une question posée par un membre du personnel : laboratoire lu, liens à afficher,
 * pseudonymes des personnes et nombre d'appels d'outils.
 *
 * <p>Une instance par question : elle n'est jamais partagée entre utilisateurs.
 * Le schéma et l'identifiant viennent du jeton (principal), jamais du navigateur ni du modèle.
 */
@Slf4j
public final class SessionOutils {

    /** Appels d'outils autorisés par question (le modèle ne peut pas boucler). */
    static final int MAX_APPELS = 3;
    /** Lignes au plus dans le résultat d'un outil. */
    static final int MAX_LIGNES = 10;
    private static final String PREFIXE_PERSONNE = "TECH_";

    private final TenantExecutor tenantExecutor;
    private final String schema;
    private final Long utilisateurId;
    private final Map<String, String> noms = new LinkedHashMap<>();
    private final Map<String, Source> liens = new LinkedHashMap<>();
    private int appels;
    private Instant lecture;

    SessionOutils(TenantExecutor tenantExecutor, String schema, Long utilisateurId) {
        this.tenantExecutor = tenantExecutor;
        this.schema = schema;
        this.utilisateurId = utilisateurId;
    }

    Long utilisateurId() {
        return utilisateurId;
    }

    /**
     * Exécute une lecture dans le schéma de l'utilisateur et retient le lien de la page concernée.
     * Toute erreur est transformée en phrase neutre : le modèle ne voit jamais de trace technique.
     */
    synchronized String lire(String titre, String lien, Supplier<String> lectureDonnees) {
        if (++appels > MAX_APPELS) {
            return "Limite de consultations atteinte pour cette question : réponds avec ce que tu as déjà.";
        }
        try {
            String resultat = schema == null
                    ? tenantExecutor.inCentral(lectureDonnees)
                    : tenantExecutor.inTenant(schema, lectureDonnees);
            liens.putIfAbsent(titre, new Source(titre, "donnees", lien, null, null));
            if (lecture == null) lecture = Instant.now();
            log.debug("Outil assistant « {} » : {}", titre, resultat);
            return resultat;
        } catch (RuntimeException ex) {
            log.warn("Outil assistant « {} » en échec : {}", titre, ex.getMessage());
            return "Donnée indisponible pour le moment.";
        }
    }

    /**
     * Pseudonyme stable d'une personne du laboratoire ({@code TECH_<id>}) : le nom n'est jamais envoyé
     * au fournisseur d'IA ; il est remis à la place du pseudonyme par le navigateur, à l'affichage.
     */
    synchronized String personne(Long id, String nom) {
        if (id == null) return "non affecté";
        String pseudonyme = PREFIXE_PERSONNE + id;
        noms.putIfAbsent(pseudonyme, nom == null || nom.isBlank() ? "Technicien " + id : nom);
        return pseudonyme;
    }

    /** Nombre écrit à la française, sans zéros inutiles : 35.000 → « 35 », 2.5 → « 2,5 ». */
    static String nombre(java.math.BigDecimal valeur) {
        if (valeur == null) return "non renseigné";
        java.math.BigDecimal v = valeur.stripTrailingZeros();
        return (v.scale() < 0 ? v.setScale(0) : v).toPlainString().replace('.', ',');
    }

    /** Montant en dirhams, deux décimales : 45 → « 45,00 MAD ». */
    static String montant(java.math.BigDecimal valeur) {
        if (valeur == null) return "0,00 MAD";
        return valeur.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString().replace('.', ',') + " MAD";
    }

    public synchronized Map<String, String> noms() {
        return Map.copyOf(noms);
    }

    public synchronized List<Source> liens() {
        return new ArrayList<>(liens.values());
    }

    /** Instant de la première lecture de données, ou {@code null} si aucun outil n'a été appelé. */
    public synchronized Instant lecture() {
        return lecture;
    }
}
