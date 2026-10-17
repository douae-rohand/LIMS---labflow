package com.backend.modules.panier.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Une ligne de panier exposée au client.
 * Si l'essai est indisponible (absent ou inactif), disponible = false
 * et les champs descriptifs (code, designation, domaineLibelle, tarif, dureeEstimee)
 * sont null.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LignePanierDto {

    private Long essaiId;
    private String code;
    private String designation;
    private String domaineLibelle;
    private BigDecimal tarif;
    private Integer dureeEstimee;
    private boolean disponible;
}
