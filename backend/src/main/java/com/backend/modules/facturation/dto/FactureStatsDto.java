package com.backend.modules.facturation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FactureStatsDto {
    private BigDecimal facture;
    private BigDecimal encaisse;
    private BigDecimal impaye;
    private Long nombreFacturesImpayees;
    private Long nombreEnRetard;
    private List<SerieMensuelleDto> serieMensuelle;
}
