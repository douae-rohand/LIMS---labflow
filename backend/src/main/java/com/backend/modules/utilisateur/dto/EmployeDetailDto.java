package com.backend.modules.utilisateur.dto;

import com.backend.modules.utilisateur.entity.StatutEmploye;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO de détail pour un employé d'un laboratoire.
 *
 * <p>Ne contient aucun mot de passe, secret 2FA ni jeton.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeDetailDto {

    private Long id;
    private String matricule;
    private String nom;
    private String prenom;
    private String nomComplet;
    private String email;
    private String telephone;
    private String role;
    private StatutEmploye statut;
    private boolean actif;
    private boolean compteConfirme;
    private Instant derniereConnexion;
    private Instant dateCreation;
    private Boolean invitationExpiree;
    private Instant invitationExpireLe;
}
