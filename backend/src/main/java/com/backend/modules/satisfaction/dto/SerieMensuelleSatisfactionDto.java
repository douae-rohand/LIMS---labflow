package com.backend.modules.satisfaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SerieMensuelleSatisfactionDto {
    private String mois;
    private Double moyenne;
}
