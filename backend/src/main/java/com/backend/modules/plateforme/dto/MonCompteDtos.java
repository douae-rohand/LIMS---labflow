package com.backend.modules.plateforme.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/** Contrats de l'espace « Mon compte » du Super Administrateur. */
public final class MonCompteDtos {

    private MonCompteDtos() {
    }

    public record MonCompteDto(
            Long id,
            String email,
            String nom,
            String prenom,
            String nomComplet,
            Instant dateCreation,
            Instant derniereConnexion) {
    }

    public record ModifierMonCompteRequest(
            @NotBlank(message = "Le nom est obligatoire") @Size(max = 100) String nom,
            @Size(max = 100) String prenom) {
    }

    /** Session ouverte ; {@code courante} vaut vrai pour la session du navigateur appelant. */
    public record MaSessionDto(Long id, String appareil, Instant ouverture, Instant expiration, boolean courante) {
    }
}
