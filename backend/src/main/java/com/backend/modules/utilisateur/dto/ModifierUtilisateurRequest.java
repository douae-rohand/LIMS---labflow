package com.backend.modules.utilisateur.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO de modification partielle d'un utilisateur (tous les champs optionnels).
 */
@Data
public class ModifierUtilisateurRequest {

    @Size(max = 100)
    private String nom;

    @Size(max = 100)
    private String prenom;

    @Email(message = "Format d'email invalide")
    @Size(max = 180)
    private String email;

    @Pattern(regexp = "^[+]?[0-9]{8,15}$", message = "Numéro de téléphone invalide")
    private String telephone;

    /** Si non null, le mot de passe sera mis à jour (après hashage). */
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    private String nouveauMotDePasse;

    private Boolean actif;
    private Boolean deuxFacteursActif;
}
