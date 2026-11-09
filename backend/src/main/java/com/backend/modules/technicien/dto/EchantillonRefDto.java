package com.backend.modules.technicien.dto;

import lombok.Builder;
import lombok.Value;

/**
 * Informations de l'échantillon visibles par le technicien.
 * ❌ motif non conformité non exposé (information interne).
 */
@Value
@Builder
public class EchantillonRefDto {
    String reference;
    String nature;
    /** true = conforme, false = non conforme, null = non évalué. */
    Boolean conformite;
    /** État de conservation constaté lors de la réception. */
    String etatConservationConstate;
}
