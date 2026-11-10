package com.backend.modules.plateforme.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class CompteUtilisateurDto {
    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String fonction;
    private String role;
    private Long laboratoireId;
    private String laboratoireNom;
    /** ACTIF ou INACTIF. Aucun secret n'est exposé. */
    private String statut;
    private Instant derniereConnexion;
    /** Vrai lorsqu'au moins un écart de permission est enregistré. */
    private boolean personnalise;
    /** Vrai lorsque la double authentification est configurée et active. */
    private boolean deuxFacteursActive;
}
