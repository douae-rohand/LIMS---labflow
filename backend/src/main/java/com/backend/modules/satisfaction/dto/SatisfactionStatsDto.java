package com.backend.modules.satisfaction.dto;

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
public class SatisfactionStatsDto {
    private Long envoyees;
    private Long repondues;
    private Double tauxReponse;
    private Double moyenneGlobale;
    private Double moyenneDelai;
    private Double moyenneClarte;
    private Double moyenneRelation;
    private Map<Integer, Long> repartitionNotes;
    private List<SerieMensuelleSatisfactionDto> serieMensuelle;
}
