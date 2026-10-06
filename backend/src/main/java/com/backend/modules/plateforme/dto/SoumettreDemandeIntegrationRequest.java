package com.backend.modules.plateforme.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class SoumettreDemandeIntegrationRequest {

    @NotBlank(message = "Le nom du laboratoire est obligatoire")
    @Size(max = 255)
    private String nomLaboratoire;

    @NotBlank(message = "La raison sociale est obligatoire")
    @Size(max = 255)
    private String raisonSociale;

    @NotEmpty(message = "Au moins un type de laboratoire est obligatoire")
    @Size(max = 8, message = "Trop de types de laboratoire")
    private List<@NotBlank(message = "Type invalide") @Size(max = 100) String> typesLaboratoire;

    @Size(max = 50)
    private String ice;

    @Size(max = 50)
    private String telephoneLaboratoire;

    @Email(message = "E-mail du laboratoire invalide")
    @Size(max = 255)
    private String emailLaboratoire;

    @Size(max = 255)
    private String siteWeb;

    @Size(max = 4000)
    private String informationsComplementaires;

    @NotBlank(message = "L'adresse est obligatoire")
    @Size(max = 1000)
    private String adresse;

    @NotBlank(message = "La ville est obligatoire")
    @Size(max = 100)
    private String ville;

    @Size(max = 100)
    private String region;

    @Size(max = 100)
    private String pays;

    @Size(max = 20)
    private String codePostal;

    @NotNull(message = "La latitude est obligatoire")
    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    private Double latitude;

    @NotNull(message = "La longitude est obligatoire")
    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    private Double longitude;

    @NotBlank(message = "Le nom de l'administrateur est obligatoire")
    @Size(max = 100)
    private String adminNom;

    @NotBlank(message = "Le prénom de l'administrateur est obligatoire")
    @Size(max = 100)
    private String adminPrenom;

    @NotBlank(message = "L'e-mail de l'administrateur est obligatoire")
    @Email(message = "E-mail de l'administrateur invalide")
    @Size(max = 255)
    private String adminEmail;

    @NotBlank(message = "Le téléphone de l'administrateur est obligatoire")
    @Size(max = 50)
    private String adminTelephone;

    @NotBlank(message = "La fonction de l'administrateur est obligatoire")
    @Size(max = 100)
    private String adminFonction;

    @NotBlank(message = "Le CIN de l'administrateur est obligatoire")
    @Size(max = 20)
    private String adminCin;
}
