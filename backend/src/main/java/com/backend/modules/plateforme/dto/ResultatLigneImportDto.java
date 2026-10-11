package com.backend.modules.plateforme.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResultatLigneImportDto {
    private int numeroLigne;
    private String nomLaboratoire;
    private String adminEmail;
    private boolean valide;
    @Builder.Default
    private List<String> erreurs = new ArrayList<>();
    private Long demandeId;
    private String numeroDemande;
    private LigneImportLaboratoireDto donnees;
}
