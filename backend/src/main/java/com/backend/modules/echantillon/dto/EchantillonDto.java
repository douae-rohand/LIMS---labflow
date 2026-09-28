package com.backend.modules.echantillon.dto;

import com.backend.modules.echantillon.entity.StatutEchantillon;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class EchantillonDto {
    private Long id;
    private String codeBarre;
    private String designation;
    private String nature;
    private Long demandeId;
    private Long receptionnaireId;
    private StatutEchantillon statut;
    private Instant dateReception;
    private LocalDate datePeremption;
    private Double quantite;
    private String unite;
    private String conditionsConservation;
    private String observations;
}
