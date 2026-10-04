package com.backend.modules.client.dto;

import lombok.Data;

@Data
public class ModifierClientProfilRequest {
    private String raisonSociale;
    private String ice;
    private String adresse;
    private Boolean consentementCndp;
}
