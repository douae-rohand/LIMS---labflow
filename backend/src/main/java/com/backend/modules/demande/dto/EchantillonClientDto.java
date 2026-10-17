package com.backend.modules.demande.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Échantillon reçu pour une demande, exposé au client.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EchantillonClientDto {

    private String reference;
    private String nature;
    private Boolean conformite;
    private String motif;
}
