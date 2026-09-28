package com.backend.modules.plateforme.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreerLaboratoireRequest {

    @NotBlank @Size(max = 50)
    @Pattern(regexp = "^[a-z0-9_]+$", message = "Le code doit être en minuscules alphanumériques (underscores autorisés)")
    private String code;

    @NotBlank @Size(max = 200)
    private String nom;

    @Size(max = 500)
    private String description;

    @Size(max = 500)
    private String adresse;

    @Size(max = 20)
    private String telephone;

    @Email @Size(max = 180)
    private String emailContact;

    @Size(max = 100)
    private String numeroAccreditation;
}
