package com.backend.modules.plateforme.dto;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModifierLaboratoireParametresRequest {
    private String raisonSociale;
    private String adresse;
    private String ville;
    private String region;
    private String codePostal;
    private String pays;
    @Pattern(regexp = "^[+]?[0-9\\s-]{8,20}$", message = "Format de téléphone invalide")
    private String telephone;
    @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "Format d'email invalide")
    private String email;
    @Pattern(regexp = "^(https?:\\/\\/)?([\\da-z\\.-]+)\\.([a-z\\.]{2,6})([\\/\\w \\.-]*)*\\/?$", message = "Format d'URL invalide")
    private String siteWeb;
    private String typeLaboratoire;
    private Double latitude;
    private Double longitude;
}
