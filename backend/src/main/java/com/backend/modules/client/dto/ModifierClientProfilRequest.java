package com.backend.modules.client.dto;

import com.backend.modules.client.entity.TypeClient;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ModifierClientProfilRequest {

    private TypeClient typeClient;

    @Size(max = 255)
    private String raisonSociale;

    @Pattern(regexp = "^[0-9]{15}$", message = "L'ICE doit contenir exactement 15 chiffres.")
    private String ice;

    @Size(max = 500)
    private String adresse;

    private Boolean consentementCndp;
}
