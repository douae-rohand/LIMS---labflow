package com.backend.modules.demande.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemandesStatsDto {
    private Long total;
    private Map<String, Long> parStatut;
    private Long nombreSurPeriode;
    private Double tauxCloture;
    private List<SerieMensuelleDemandeDto> serieMensuelle;
}
