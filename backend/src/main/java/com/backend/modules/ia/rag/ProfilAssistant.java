package com.backend.modules.ia.rag;

import java.util.Set;

/**
 * Qui pose la question, et donc ce que l'assistant a le droit de lire.
 *
 * @param cleLimite      clé du limiteur ({@code u:<id>} ou {@code ip:<adresse>})
 * @param maxParHeure    nombre de questions autorisées par heure
 * @param audiences      pages de documentation lisibles (métadonnée {@code audience} du corpus)
 * @param tenants        catalogues lisibles ({@code commun} + schémas des laboratoires)
 * @param description    phrase qui présente l'interlocuteur au modèle
 * @param laboratoires   noms lisibles des laboratoires concernés, pour le modèle
 * @param sansInformation réponse fixe quand la documentation ne contient pas la réponse
 * @param espace         espace de l'interlocuteur, pour construire les liens : {@code public}, {@code client},
 *                       {@code super-admin} ou le rôle du personnel ({@code accueil}, {@code responsable}…)
 */
public record ProfilAssistant(
        String cleLimite,
        int maxParHeure,
        Set<String> audiences,
        Set<String> tenants,
        String description,
        String laboratoires,
        String sansInformation,
        String espace) {
}
