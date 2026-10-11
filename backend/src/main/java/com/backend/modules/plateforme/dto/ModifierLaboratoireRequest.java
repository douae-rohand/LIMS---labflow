package com.backend.modules.plateforme.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ModifierLaboratoireRequest {

    @NotBlank(message = "Le nom du laboratoire est obligatoire")
    @Size(max = 255)
    private String nom;

    @Size(max = 50)
    private String ice;

    @Size(max = 500)
    private String typeLaboratoire;

    @Size(max = 4000)
    private String adresse;

    @Size(max = 100)
    private String ville;

    @Size(max = 100)
    private String region;

    @Size(max = 100)
    private String pays;

    @Size(max = 20)
    private String codePostal;

    @Size(max = 50)
    private String telephone;

    @Email(message = "L'adresse e-mail du laboratoire est invalide")
    @Size(max = 255)
    private String email;

    @Size(max = 255)
    private String siteWeb;

    @DecimalMin(value = "-90.0", message = "La latitude est invalide")
    @DecimalMax(value = "90.0", message = "La latitude est invalide")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "La longitude est invalide")
    @DecimalMax(value = "180.0", message = "La longitude est invalide")
    private Double longitude;
}
