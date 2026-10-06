package com.backend.modules.utilisateur.dto;

import com.backend.modules.utilisateur.entity.RoleUtilisateur;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * DTO de création d'un utilisateur.
 *
 * <p>Deux flux sont possibles :
 * <ul>
 *   <li><b>Flux invitation</b> : {@code motDePasse} est null → le compte est créé inactif
 *       et un email d'activation est envoyé pour que l'utilisateur définisse son propre mot de passe.</li>
 *   <li><b>Flux direct</b> : {@code motDePasse} est fourni → le compte est créé actif.</li>
 * </ul>
 */
@Data
public class CreerUtilisateurRequest {

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 100)
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    @Size(max = 100)
    private String prenom;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    @Size(max = 180)
    private String email;

    /**
     * Mot de passe initial. Optionnel.
     * Si null → flux invitation : email d'activation envoyé, compte créé inactif.
     * Si renseigné → compte créé actif immédiatement.
     */
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    private String motDePasse;

    @Pattern(regexp = "^[+]?[0-9]{8,15}$", message = "Numéro de téléphone invalide")
    private String telephone;

    @NotNull(message = "Le rôle est obligatoire")
    private RoleUtilisateur role;

    /**
     * Obligatoire pour le personnel de laboratoire sauf si l'admin connecté est lui-même
     * rattaché à un labo (le service utilise alors automatiquement le labo de l'admin).
     * Interdit pour CLIENT et SUPER_ADMINISTRATEUR.
     */
    private Long laboratoireId;

    // Champs CLIENT uniquement
    private String raisonSociale;
    private String ice;
    private String adresse;
    private Boolean consentementCndp;
}
