package com.backend.modules.plateforme.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class ProfilCompteDto {

    private final Long id;
    private final String nom;
    private final String prenom;
    private final String email;
    private final String telephone;
    private final String fonction;
    private final String role;
    private final boolean doubleAuthentification;
    private final Instant derniereConnexion;
}
