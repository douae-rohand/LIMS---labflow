package com.backend.modules.planification.dto;

import com.backend.modules.planification.entity.StatutPlanification;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PlanificationDto {
    private Long id;
    private Long demandeId;
    private Long technicienId;
    private LocalDate dateDebutPrevue;
    private LocalDate dateFinPrevue;
    private Instant dateDebutEffective;
    private Instant dateFinEffective;
    private StatutPlanification statut;
    private String notes;
}
