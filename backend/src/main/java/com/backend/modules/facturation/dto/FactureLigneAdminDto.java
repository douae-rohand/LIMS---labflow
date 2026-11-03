package com.backend.modules.facturation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FactureLigneAdminDto {
    private Long id;
    private String designation;
    private BigDecimal montant;
    private Long ligneEssaiId;
}
