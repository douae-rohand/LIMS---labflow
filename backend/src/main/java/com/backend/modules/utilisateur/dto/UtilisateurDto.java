package com.backend.modules.utilisateur.dto;

import com.backend.modules.utilisateur.entity.RoleUtilisateur;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO de lecture d'un utilisateur (réponse API).
 * Ne contient jamais le mot de passe.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UtilisateurDto {

    private Long id;
    private String nom;
    private String prenom;
    private String nomComplet;
    private String email;
    private String telephone;
    private RoleUtilisateur role;
    private boolean actif;
    private boolean deuxFacteursActif;
    private Instant dateCreation;
    private Instant derniereConnexion;
}
