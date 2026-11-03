package com.backend.modules.plateforme.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreerAdministrateurLaboratoireRequest {

    @NotNull(message = "Le laboratoire est obligatoire")
    private Long laboratoireId;

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 100)
    private String nom;

    @Size(max = 100)
    private String prenom;

    @NotBlank(message = "L'e-mail est obligatoire")
    @Email(message = "L'e-mail est invalide")
    @Size(max = 255)
    private String email;

    @Size(max = 50)
    private String telephone;

    @Size(max = 100)
    private String fonction;
}
