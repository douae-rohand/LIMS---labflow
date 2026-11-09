package com.backend.modules.technicien.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.List;

/** Regroupement des essais d'un technicien pour un jour donné (vue planning). */
@Value
@Builder
public class PlanningJourDto {
    LocalDate date;
    List<LigneEssaiTechnicienDto> essais;
}
