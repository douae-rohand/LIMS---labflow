package com.backend.modules.ia.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Échanges entre l'espace client et l'assistant IA. */
public final class AssistantDtos {

    private AssistantDtos() {}

    /** Un message déjà échangé, renvoyé par le navigateur pour garder le fil de la discussion. */
    public record MessageHistorique(
            @NotBlank @Pattern(regexp = "client|assistant") String role,
            @NotBlank @Size(max = 2000) String texte) {}

    public record QuestionRequest(
            @NotBlank(message = "La question est obligatoire.")
            @Size(min = 3, max = 500, message = "La question doit faire entre 3 et 500 caractères.")
            String question,
            @Size(max = 6, message = "L'historique est limité aux 6 derniers messages.")
            List<@Valid MessageHistorique> historique) {}

    /**
     * Source affichée sous la réponse.
     *
     * @param lien      page de l'application à ouvrir (calculée par le serveur, jamais par le modèle), ou {@code null}
     * @param prix      tarif de l'analyse (fiche de catalogue), ou {@code null}
     * @param lienAjout lien qui ajoute l'analyse au panier (visiteur et client), ou {@code null}
     */
    public record Source(String titre, String source, String lien, String prix, String lienAjout) {}

    /**
     * Réponse de l'assistant.
     *
     * @param noms     noms des personnes à afficher à la place des pseudonymes {@code TECH_<id>} de la réponse
     *                 (le fournisseur d'IA ne voit que les pseudonymes) ; vide sans lecture de données
     * @param donneesA instant de lecture des données du laboratoire, ou {@code null} si aucune n'a été lue
     */
    public record ReponseAssistant(String reponse, List<Source> sources,
                                   java.util.Map<String, String> noms, java.time.Instant donneesA) {
        public ReponseAssistant(String reponse, List<Source> sources) {
            this(reponse, sources, java.util.Map.of(), null);
        }
    }

    public record EtatIndex(boolean pret, java.util.Map<String, Integer> passages) {}
}
