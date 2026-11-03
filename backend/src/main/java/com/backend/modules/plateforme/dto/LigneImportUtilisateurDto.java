package com.backend.modules.plateforme.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LigneImportUtilisateurDto {
    private int numeroLigne;
    private String nom;
    private String prenom;
    private String email;
    private String role;
    private String laboratoire;
    private String telephone;
    private String fonction;
}
