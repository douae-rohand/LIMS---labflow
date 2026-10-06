package com.backend.modules.auth.dto;

import com.backend.modules.client.entity.TypeClient;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Corps de POST /api/auth/inscription.
 *
 * Règles de validation côté serveur (miroir de la politique frontend) :
 *  - Politique mot de passe : >= 10 chars, maj, min, chiffre
 *  - ICE : exactement 15 chiffres si fourni (ENTREPRISE uniquement)
 *  - raison_sociale : obligatoire pour ENTREPRISE
 *  - consentement_cndp : obligatoire = true
 *  - telephone : obligatoire pour les deux types
 */
@Data
public class InscriptionRequest {

    // ------------------------------------------------------------------
    // Identité
    // ------------------------------------------------------------------

    @NotBlank(message = "Le nom est obligatoire.")
    @Size(max = 100, message = "Le nom ne peut pas dépasser 100 caractères.")
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire.")
    @Size(max = 100, message = "Le prénom ne peut pas dépasser 100 caractères.")
    private String prenom;

    @NotBlank(message = "L'email est obligatoire.")
    @Email(message = "Format d'email invalide.")
    @Size(max = 255, message = "L'email ne peut pas dépasser 255 caractères.")
    private String email;

    @NotBlank(message = "Le téléphone est obligatoire.")
    @Pattern(
        regexp = "^\\+?[0-9]{8,15}$",
        message = "Le numéro de téléphone est invalide (8 à 15 chiffres, + optionnel)."
    )
    private String telephone;

    // ------------------------------------------------------------------
    // Mot de passe (politique identique au backend MotDePasseValidator)
    // ------------------------------------------------------------------

    @NotBlank(message = "Le mot de passe est obligatoire.")
    @Size(min = 10, message = "Le mot de passe doit contenir au moins 10 caractères.")
    @Pattern(regexp = ".*[a-z].*", message = "Le mot de passe doit contenir au moins une lettre minuscule.")
    @Pattern(regexp = ".*[A-Z].*", message = "Le mot de passe doit contenir au moins une lettre majuscule.")
    @Pattern(regexp = ".*[0-9].*", message = "Le mot de passe doit contenir au moins un chiffre.")
    private String motDePasse;

    // ------------------------------------------------------------------
    // Type de client et informations professionnelles
    // ------------------------------------------------------------------

    @NotNull(message = "Le type de client est obligatoire (PARTICULIER ou ENTREPRISE).")
    private TypeClient typeClient;

    /**
     * Obligatoire pour les ENTREPRISE, ignoré pour les PARTICULIER.
     * Validé par @AssertTrue ci-dessous.
     */
    @Size(max = 255, message = "La raison sociale ne peut pas dépasser 255 caractères.")
    private String raisonSociale;

    /**
     * ICE marocain : exactement 15 chiffres.
     * Facultatif même pour les entreprises, mais s'il est fourni, il doit être valide.
     */
    @Pattern(
        regexp = "^[0-9]{15}$",
        message = "L'ICE doit contenir exactement 15 chiffres."
    )
    private String ice;

    @Size(max = 500, message = "L'adresse ne peut pas dépasser 500 caractères.")
    private String adresse;

    // ------------------------------------------------------------------
    // Consentement CNDP
    // ------------------------------------------------------------------

    @AssertTrue(message = "Le consentement à la politique de confidentialité est obligatoire.")
    private boolean consentementCndp;

    // ------------------------------------------------------------------
    // Validation croisée : raison sociale obligatoire pour ENTREPRISE
    // ------------------------------------------------------------------

    /**
     * Vérifie que la raison sociale est renseignée pour une ENTREPRISE.
     * Bean Validation n'évalue cette contrainte QUE si typeClient est non-null.
     */
    @AssertTrue(message = "La raison sociale est obligatoire pour une entreprise.")
    public boolean isRaisonSocialeValide() {
        if (typeClient == null) return true; // autre contrainte @NotNull couvre ce cas
        if (typeClient == TypeClient.ENTREPRISE) {
            return raisonSociale != null && !raisonSociale.isBlank();
        }
        return true;
    }
}
