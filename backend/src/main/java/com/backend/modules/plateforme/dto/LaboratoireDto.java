package com.backend.modules.plateforme.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class LaboratoireDto {
    private Long id;
    private String code;
    private String nom;
    private String description;
    private String schemaName;
    private String adresse;
    private String ville;
    private String region;
    private String pays;
    private String codePostal;
    private String ice;
    private String typeLaboratoire;
    @Builder.Default
    private List<String> typesLaboratoire = new ArrayList<>();
    private String telephone;
    private String emailContact;
    private String siteWeb;
    private Double latitude;
    private Double longitude;
    private String numeroAccreditation;
    /** ACTIF ou INACTIF. Distinct du statut d'une demande d'intégration. */
    private String statut;
    private boolean actif;
    private Instant dateCreation;
    private AdministrateurLaboratoireDto administrateur;
}
