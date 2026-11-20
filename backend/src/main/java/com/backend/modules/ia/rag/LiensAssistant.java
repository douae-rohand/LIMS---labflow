package com.backend.modules.ia.rag;

import com.backend.modules.ia.dto.AssistantDtos.Source;
import org.springframework.ai.document.Document;

import java.util.Map;

/**
 * Liens affichés sous les réponses de l'assistant.
 *
 * <p>Les adresses sont construites ici, à partir des métadonnées des passages et de l'espace de
 * l'interlocuteur : le modèle n'écrit jamais d'URL, il ne peut donc pas en inventer une.
 * Chaque lien mène à une page présente dans le menu de l'espace concerné (shells du frontend).
 */
final class LiensAssistant {

    private LiensAssistant() {}

    /** Page de l'application par espace, puis par « fichier#section » (ou « fichier#* » pour tout le fichier). */
    private static final Map<String, Map<String, String>> PAGES = Map.of(
            "public", Map.ofEntries(
                    Map.entry("01-passer-une-demande.md#Préparer le panier", "/laboratoires"),
                    Map.entry("07-compte-securite.md#Se connecter", "/login"),
                    Map.entry("07-compte-securite.md#Mot de passe oublié", "/mot-de-passe-oublie"),
                    Map.entry("09-faq.md#Comment connaître le prix d'une analyse ?", "/laboratoires"),
                    Map.entry("10-decouvrir-labflow.md#Quels laboratoires utilisent LabFlow ?", "/laboratoires"),
                    Map.entry("11-visiteurs.md#Créer un compte client", "/inscription"),
                    Map.entry("11-visiteurs.md#Voir les laboratoires et leurs analyses", "/laboratoires"),
                    Map.entry("11-visiteurs.md#Se connecter", "/login"),
                    Map.entry("11-visiteurs.md#Intégrer son laboratoire à LabFlow", "/integration"),
                    Map.entry("11-visiteurs.md#Venir directement au laboratoire", "/carte")),
            "client", Map.ofEntries(
                    Map.entry("01-passer-une-demande.md#Préparer le panier", "/app/laboratoires"),
                    Map.entry("01-passer-une-demande.md#Envoyer la demande", "/app/panier"),
                    Map.entry("01-passer-une-demande.md#Les statuts d'une demande", "/app/demandes"),
                    Map.entry("01-passer-une-demande.md#Suivre ses demandes", "/app/demandes"),
                    Map.entry("04-paiement-facture.md#*", "/app/factures"),
                    Map.entry("05-rapport-resultats.md#Où trouver son rapport", "/app/rapports"),
                    Map.entry("05-rapport-resultats.md#Rapport partiel", "/app/rapports"),
                    Map.entry("07-compte-securite.md#Modifier ses informations", "/app/profil"),
                    Map.entry("08-enquete-satisfaction.md#*", "/app/enquetes"),
                    Map.entry("09-faq.md#Comment connaître le prix d'une analyse ?", "/app/laboratoires"),
                    Map.entry("09-faq.md#Comment savoir si mon rapport est prêt ?", "/app/rapports"),
                    Map.entry("10-decouvrir-labflow.md#Quels laboratoires utilisent LabFlow ?", "/app/laboratoires")),
            "accueil", Map.ofEntries(
                    Map.entry("20-personnel-commun.md#Profil et mot de passe", "/app/profil"),
                    Map.entry("21-accueil.md#Enregistrer un patient venu sur place", "/app/nouvelle-demande"),
                    Map.entry("21-accueil.md#Réceptionner un échantillon", "/app/reception"),
                    Map.entry("21-accueil.md#Imprimer les étiquettes", "/app/reception"),
                    Map.entry("21-accueil.md#Encaisser un paiement", "/app/caisse"),
                    Map.entry("21-accueil.md#Annuler un encaissement", "/app/caisse"),
                    Map.entry("21-accueil.md#Journal de caisse", "/app/caisse"),
                    Map.entry("21-accueil.md#Rapport signé avec facture impayée", "/app/caisse"),
                    Map.entry("21-accueil.md#Suivre les demandes", "/app/suivi-demandes")),
            "responsable", Map.ofEntries(
                    Map.entry("20-personnel-commun.md#Profil et mot de passe", "/app/profil"),
                    Map.entry("22-responsable.md#Accepter ou refuser une demande", "/app/demandes-clients"),
                    Map.entry("22-responsable.md#Affecter les analyses (planification)", "/app/planification"),
                    Map.entry("22-responsable.md#Valider les résultats", "/app/validation-resultats"),
                    Map.entry("22-responsable.md#Signer le rapport", "/app/rapports-laboratoire"),
                    Map.entry("22-responsable.md#Tableau de suivi", "/app/suivi"),
                    Map.entry("22-responsable.md#Score d'anomalie des résultats", "/app/validation-resultats"),
                    Map.entry("22-responsable.md#Synthèse IA des résultats", "/app/rapports-laboratoire"),
                    Map.entry("22-responsable.md#Exports", "/app/exports")),
            "technicien", Map.ofEntries(
                    Map.entry("20-personnel-commun.md#Profil et mot de passe", "/app/profil"),
                    Map.entry("23-technicien.md#Tableau de bord", "/app"),
                    Map.entry("23-technicien.md#Réaliser un essai et saisir le résultat", "/app/essais"),
                    Map.entry("23-technicien.md#Essai non réalisable", "/app/essais"),
                    Map.entry("23-technicien.md#Résultat retourné", "/app/essais"),
                    Map.entry("23-technicien.md#Planning", "/app/planning"),
                    Map.entry("23-technicien.md#Import CSV", "/app/import")),
            "administrateur", Map.ofEntries(
                    Map.entry("20-personnel-commun.md#Profil et mot de passe", "/app/profil"),
                    Map.entry("20-personnel-commun.md#Journal d'audit", "/app/audit"),
                    Map.entry("24-administrateur.md#Gérer les employés", "/app/employes"),
                    Map.entry("24-administrateur.md#Domaines de compétence des techniciens", "/app/employes"),
                    Map.entry("24-administrateur.md#Gérer le catalogue", "/app/catalogue"),
                    Map.entry("24-administrateur.md#Stock", "/app/stock"),
                    Map.entry("24-administrateur.md#Facturation", "/app/facturation"),
                    Map.entry("24-administrateur.md#Satisfaction", "/app/satisfaction"),
                    Map.entry("24-administrateur.md#Journal d'audit et exports", "/app/audit")),
            "super-admin", Map.ofEntries(
                    Map.entry("07-compte-securite.md#Modifier ses informations", "/app/profil"),
                    Map.entry("25-super-admin.md#Étudier une demande d'intégration", "/app/gestion-laboratoires"),
                    Map.entry("25-super-admin.md#Gérer les laboratoires", "/app/gestion-laboratoires"),
                    Map.entry("25-super-admin.md#Commission mensuelle", "/app/gestion-laboratoires"),
                    Map.entry("25-super-admin.md#Comptes administrateurs", "/app/utilisateurs"),
                    Map.entry("25-super-admin.md#Double authentification par e-mail", "/app/parametres-plateforme"),
                    Map.entry("25-super-admin.md#Journal d'audit de la plateforme", "/app/journal-audit"),
                    Map.entry("25-super-admin.md#Assistant IA", "/app/parametres-plateforme")));

    static Source source(Document d, String espace) {
        Map<String, Object> meta = d.getMetadata();
        String titre = String.valueOf(meta.getOrDefault("titre", ""));
        String fichier = String.valueOf(meta.getOrDefault("source", ""));
        if ("catalogue".equals(fichier)) {
            return ficheCatalogue(titre, meta, espace);
        }
        return new Source(titre, fichier, pageDocumentation(fichier, titre, espace), null, null);
    }

    private static Source ficheCatalogue(String titre, Map<String, Object> meta, String espace) {
        Object laboratoireId = meta.get("laboratoireId");
        Object essaiId = meta.get("essaiId");
        String prix = meta.get("prix") == null ? null : meta.get("prix").toString();
        String lien = null;
        String lienAjout = null;
        if (laboratoireId != null) {
            switch (espace) {
                case "public", "client" -> {
                    lien = ("public".equals(espace) ? "/laboratoires/" : "/app/laboratoires/") + laboratoireId;
                    // Visiteur : la page renvoie d'abord vers la connexion, puis ajoute l'analyse au panier
                    if (essaiId != null) lienAjout = lien + "?ajouter=" + essaiId;
                }
                case "administrateur" -> lien = "/app/catalogue";
                case "super-admin" -> lien = "/app/gestion-laboratoires";
                default -> { /* accueil, responsable, technicien : pas de page catalogue dans leur menu */ }
            }
        }
        return new Source(titre, "catalogue", lien, prix, lienAjout);
    }

    private static String pageDocumentation(String fichier, String titre, String espace) {
        Map<String, String> pages = PAGES.getOrDefault(espace, Map.of());
        int tiret = titre.lastIndexOf(" – ");
        String section = tiret < 0 ? "" : titre.substring(tiret + 3);
        String page = pages.get(fichier + "#" + section);
        return page != null ? page : pages.get(fichier + "#*");
    }
}
