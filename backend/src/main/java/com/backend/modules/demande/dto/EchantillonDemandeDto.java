package com.backend.modules.demande.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EchantillonDemandeDto {
    private Long id;
    private String reference;
    private String nature;
    private Instant dateReception;
    private Boolean conformite;
}
