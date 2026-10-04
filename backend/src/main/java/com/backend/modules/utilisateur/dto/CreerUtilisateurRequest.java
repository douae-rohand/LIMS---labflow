package com.backend.modules.utilisateur.dto;

import com.backend.modules.utilisateur.entity.RoleUtilisateur;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * DTO de création d'un utilisateur.
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

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    private String motDePasse;

    @Pattern(regexp = "^[+]?[0-9]{8,15}$", message = "Numéro de téléphone invalide")
    private String telephone;

    @NotNull(message = "Le rôle est obligatoire")
    private RoleUtilisateur role;

    /** Obligatoire pour le personnel de laboratoire. Interdit pour CLIENT. */
    private Long laboratoireId;

    private String raisonSociale;
    private String ice;
    private String adresse;
    private Boolean consentementCndp;
}
