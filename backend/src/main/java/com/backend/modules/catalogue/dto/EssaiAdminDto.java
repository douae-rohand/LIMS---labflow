package com.backend.modules.catalogue.dto;

import com.backend.modules.catalogue.dto.ExigenceEchantillonDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EssaiAdminDto {
    private Long id;
    private String code;
    private String designation;
    private String description;
    private String methode;
    private BigDecimal tarif;
    private Integer dureeEstimee;
    private String unite;
    private BigDecimal limiteMin;
    private BigDecimal limiteMax;
    private BigDecimal seuilCritiqueMin;
    private BigDecimal seuilCritiqueMax;
    private Boolean actif;
    private Long domaineId;
    private String domaineLibelle;
    private List<ExigenceEchantillonDto> exigences;
}
