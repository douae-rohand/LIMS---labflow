package com.backend.modules.plateforme.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
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

    /** Données publiques utiles aux formulaires (inscription, intégration, mots de passe) et à la landing. */
    public record ParametresPublics(PolitiqueMotDePasse politiqueMotDePasse, List<String> typesLaboratoire,
                                    Identite identite) {
    }

    /** Identité de la plateforme : coordonnées et expéditeur des e-mails. */
    public record Identite(
            @NotBlank(message = "Le nom de la plateforme est obligatoire") @Size(max = 100) String nom,
            @Email(message = "Adresse e-mail de contact invalide") @Size(max = 255) String emailContact,
            @Size(max = 50) String telephone,
            @Size(max = 500) String adresse,
            @NotBlank(message = "Le nom d'expéditeur est obligatoire") @Size(max = 100) String expediteurNom) {
    }

    /** Taux de TVA de la plateforme, en pourcentage (les prix du catalogue sont saisis TTC). */
    public record Facturation(
            @NotNull(message = "Le taux de TVA est obligatoire")
            @DecimalMin(value = "0.00", message = "Le taux de TVA ne peut pas être négatif")
            @DecimalMax(value = "100.00", message = "Le taux de TVA ne peut pas dépasser 100 %")
            BigDecimal tauxTva) {
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
