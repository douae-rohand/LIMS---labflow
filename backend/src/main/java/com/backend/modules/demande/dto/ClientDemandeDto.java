package com.backend.modules.demande.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientDemandeDto {
    private Long id;
    private String code;
    private String raisonSociale;
    private String ice;
    private String adresse;
    private String nomComplet;
    private String email;
    private String telephone;
}
