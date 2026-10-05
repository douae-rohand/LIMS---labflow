package com.backend.modules.plateforme.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Compteurs landing calculés à partir de sources réelles (tables centrales + enum métier).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LandingStatistiquesDto {
    /** {@code COUNT(*)} sur la table centrale {@code role}. */
    private long nombreRoles;
    /** {@code COUNT(*)} sur {@code laboratoire} où {@code statut = 'ACTIF'}. */
    private long nombreLaboratoiresActifs;
    /** Nombre de valeurs de {@link com.backend.modules.demande.entity.StatutDemande}. */
    private long nombreStatutsDemande;
}
