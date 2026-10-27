package com.backend.modules.demande.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientDemandeDto {
    private Long id;
    private String code;
    private String nomComplet;
    private LocalDate dateNaissance;
    private String telephone;
    private String email;
}
