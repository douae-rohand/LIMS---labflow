package com.backend.modules.plateforme.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatistiquesPlateformeDto {
    private long totalLaboratoires;
    private long laboratoiresActifs;
    private long laboratoiresDesactives;
    /** Demandes encore ouvertes : en attente ou en cours de validation. */
    private long demandesEnAttente;
    private long totalDemandes;
}
