package com.backend.modules.plateforme.dto;

import com.backend.modules.plateforme.entity.StatutIntegration;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemandeIntegrationDto {
    private Long id;
    private String numero;
    private StatutIntegration statut;
    private Instant dateSoumission;
    private Instant dateTraitement;
    private String motifRefus;
    private String commentaireAdmin;

    private String nomLaboratoire;
    private String raisonSociale;
    private String typeLaboratoire;
    @Builder.Default
    private List<String> typesLaboratoire = new ArrayList<>();
    private String ice;
    private String telephoneLaboratoire;
    private String emailLaboratoire;
    private String siteWeb;
    private String informationsComplementaires;

    private String adresse;
    private String ville;
    private String region;
    private String pays;
    private String codePostal;
    private Double latitude;
    private Double longitude;

    private String adminNom;
    private String adminPrenom;
    private String adminEmail;
    private String adminTelephone;
    private String adminFonction;
    private String adminCin;

    /** Champs historiques conservés pour compatibilité. */
    private String emailRepresentant;
    private String nomRepresentant;
    private String telephoneRepresentant;
    private String message;

    private Long laboratoireId;
    private String laboratoireCode;
    private String nomSchema;

    @Builder.Default
    private List<DocumentIntegrationDto> documents = new ArrayList<>();
}
