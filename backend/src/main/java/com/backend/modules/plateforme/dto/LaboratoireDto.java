package com.backend.modules.plateforme.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class LaboratoireDto {
    private Long id;
    private String code;
    private String nom;
    private String description;
    private String schemaName;
    private String adresse;
    private String telephone;
    private String emailContact;
    private String numeroAccreditation;
    private boolean actif;
    private Instant dateCreation;
}
