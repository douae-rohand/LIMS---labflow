package com.backend.modules.plateforme.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Payload public de la landing : agrège uniquement des données déjà présentes
 * en base centrale ou dans l'enum métier {@code StatutDemande}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LandingPublicDto {
    private LandingStatistiquesDto statistiques;
    private List<RolePublicDto> roles;
    private List<LaboratoirePublicDto> laboratoires;
    private List<String> statutsDemande;
}
