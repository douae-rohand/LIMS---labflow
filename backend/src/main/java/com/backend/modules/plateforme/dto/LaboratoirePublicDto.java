package com.backend.modules.plateforme.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaboratoirePublicDto {
    private Long id;
    private String code;
    private String raisonSociale;
    private String ville;
    private String adresse;
    private String telephone;
    private String email;
    private String ice;
    private String statut;
    private Double latitude;
    private Double longitude;
}
