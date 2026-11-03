package com.backend.modules.demande.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SerieMensuelleDemandeDto {
    private String mois;
    private Long nombre;
}
