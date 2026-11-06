package com.backend.modules.plateforme.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaboratoireParametresDto {
    private Long id;
    private String code;
    private String raisonSociale;
    private String adresse;
    private String ville;
    private String region;
    private String codePostal;
    private String pays;
    private String telephone;
    private String email;
    private String siteWeb;
    private String typeLaboratoire;
    private Double latitude;
    private Double longitude;
}
