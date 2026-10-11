package com.backend.modules.plateforme.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ModifierAdministrateurLaboratoireRequest {

    @NotNull(message = "L'administrateur à modifier est obligatoire")
    private Long id;

    @NotBlank(message = "Le nom de l'administrateur est obligatoire")
    @Size(max = 100)
    private String nom;

    @NotBlank(message = "Le prénom de l'administrateur est obligatoire")
    @Size(max = 100)
    private String prenom;

    @NotBlank(message = "L'adresse e-mail de l'administrateur est obligatoire")
    @Email(message = "L'adresse e-mail de l'administrateur est invalide")
    @Size(max = 255)
    private String email;

    @Size(max = 50)
    private String telephone;

    @Size(max = 100)
    private String fonction;

    @Size(max = 20)
    private String cin;
}
