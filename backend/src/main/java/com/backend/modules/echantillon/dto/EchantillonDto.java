package com.backend.modules.echantillon.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long receptionneurId;

    private Instant dateReception;
    private LocalDate datePeremption;
    private Double quantite;
    private String unite;
    private String etatConservationConstate;
    private Boolean conformite;
    private String observations;
}
