package com.backend.modules.plateforme.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ModifierProfilRequest {

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 100, message = "Le nom ne peut pas dépasser 100 caractères")
    private String nom;

    @Size(max = 100, message = "Le prénom ne peut pas dépasser 100 caractères")
    private String prenom;

    @Size(max = 50, message = "Le téléphone ne peut pas dépasser 50 caractères")
    private String telephone;

    @Size(max = 100, message = "La fonction ne peut pas dépasser 100 caractères")
    private String fonction;
}
