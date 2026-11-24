package com.backend.modules.demande.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyseDemandeeDto {
    private Long id;
    private String code;
    private String designation;
    private String methode;
    private String unite;
    private BigDecimal tarif;
    private BigDecimal montant;
    private String statut;
    /** ATTENDU, CONFORME ou NON_CONFORME : échantillon rattaché à l'analyse. */
    private String etatEchantillon;
    /** Vrai si un technicien est affecté à l'analyse. */
    private boolean affectee;
}
