package com.backend.modules.technicien.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

/** Exigence d'échantillon de l'essai catalogue, visible par le technicien. */
@Value
@Builder
public class ExigenceEchantillonRefDto {
    String nature;
    String etatConservation;
    BigDecimal quantiteMinimale;
    String unite;
}
