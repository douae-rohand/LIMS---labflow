package com.backend.modules.plateforme.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class CompteAdministrateurDto {
    private Long id;
    private Long laboratoireId;
    private String laboratoireNom;
    private String laboratoireStatut;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String fonction;
    private String statut;
    private Instant derniereConnexion;
    private boolean mustChangePassword;
}
