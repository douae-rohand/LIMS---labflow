package com.backend.modules.plateforme.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Contrats d'échange de l'écran « Paramètres de la plateforme ». */
public final class ParametresPlateformeDtos {

    private ParametresPlateformeDtos() {
    }

    /** Paramètres de sécurité, en lecture comme en écriture. */
    public record Securite(
            @NotNull List<@NotBlank String> roles2faObligatoire,
            @Min(value = 10, message = "La longueur minimale ne peut pas descendre sous 10 caractères")
            @Max(value = 64, message = "La longueur minimale ne peut pas dépasser 64 caractères")
            int longueurMinMotDePasse,
            boolean exigerCaractereSpecial,
            @Min(value = 1, message = "La durée de session doit être d'au moins 1 jour")
            @Max(value = 30, message = "La durée de session ne peut pas dépasser 30 jours")
            int dureeSessionJours) {
    }

    /** Politique de mot de passe exposée publiquement aux formulaires. */
    public record PolitiqueMotDePasse(int longueurMin, boolean exigerCaractereSpecial) {
    }

    /** Données publiques utiles aux formulaires (inscription, intégration, mots de passe). */
    public record ParametresPublics(PolitiqueMotDePasse politiqueMotDePasse, List<String> typesLaboratoire) {
    }

    public record EnregistrerTypeDocument(
            @NotBlank(message = "Le libellé est obligatoire") @Size(max = 255) String libelle,
            @Size(max = 255) String raison,
            boolean obligatoire,
            boolean actif,
            @Min(0) @Max(999) int ordre) {
    }

    public record TypeLaboratoire(Long id, String libelle, boolean actif, int ordre) {
    }

    public record CreerTypeLaboratoire(
            @NotBlank(message = "Le libellé est obligatoire") @Size(max = 100) String libelle,
            @Min(0) @Max(999) int ordre) {
    }

    public record ModifierTypeLaboratoire(boolean actif, @Min(0) @Max(999) int ordre) {
    }
}
