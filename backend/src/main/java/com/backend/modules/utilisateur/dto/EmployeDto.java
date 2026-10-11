package com.backend.modules.utilisateur.dto;

import com.backend.modules.utilisateur.entity.StatutEmploye;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO de lecture pour un employé d'un laboratoire.
 *
 * <p>Ne contient aucun mot de passe, secret 2FA ni jeton.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeDto {

    private Long id;
    private String matricule;
    private String nom;
    private String prenom;
    private String nomComplet;
    private String email;
    private String telephone;
    private String role;
    private StatutEmploye statut;
    private Instant derniereConnexion;
    private Instant dateCreation;
}
