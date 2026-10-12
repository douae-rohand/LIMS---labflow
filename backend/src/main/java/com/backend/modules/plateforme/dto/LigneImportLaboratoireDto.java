package com.backend.modules.plateforme.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LigneImportLaboratoireDto {
    private Integer numeroLigne;
    private String nomLaboratoire;
    private String raisonSociale;
    private String typesLaboratoire;
    private String ice;
    private String ville;
    private String adresse;
    private String region;
    private String pays;
    private String codePostal;
    private String telephoneLaboratoire;
    private String emailLaboratoire;
    private String siteWeb;
    private String adminNom;
    private String adminPrenom;
    private String adminEmail;
    private String adminTelephone;
    private String adminFonction;
    private String adminCin;
    private String latitude;
    private String longitude;
}
