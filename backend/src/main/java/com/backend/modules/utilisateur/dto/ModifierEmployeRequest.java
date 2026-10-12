package com.backend.modules.utilisateur.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Requête de modification partielle d'un employé par l'administrateur.
 * Tous les champs sont optionnels (null = inchangé).
 */
@Data
public class ModifierEmployeRequest {

    @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères")
    private String nom;

    @Size(max = 100, message = "Le prénom ne doit pas dépasser 100 caractères")
    private String prenom;

    @Pattern(regexp = "^[+]?[0-9]{8,15}$", message = "Numéro de téléphone invalide")
    private String telephone;

    @Size(max = 50, message = "Le rôle ne doit pas dépasser 50 caractères")
    private String role;

    @Email(message = "Format d'email invalide")
    @Size(max = 180, message = "L'email ne doit pas dépasser 180 caractères")
    private String email;
}
