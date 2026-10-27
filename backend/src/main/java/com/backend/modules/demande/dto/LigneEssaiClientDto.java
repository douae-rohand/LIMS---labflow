package com.backend.modules.demande.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Une ligne d'essai exposée au client dans le détail d'une demande.
 * N'expose jamais de limites de référence ni de seuils.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LigneEssaiClientDto {

    private Long essaiId;
    private String code;
    private String designation;
    private String domaineLibelle;
    private BigDecimal tarif;
    private String statut;
}
