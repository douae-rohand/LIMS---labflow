package com.backend.modules.facturation.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record DemandeFacturableDto(
        Long id,
        String numero,
        String titre,
        String clientNom,
        String statut,
        Instant dateSoumission,
        BigDecimal total) {
}
