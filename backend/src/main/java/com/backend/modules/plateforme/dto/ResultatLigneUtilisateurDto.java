package com.backend.modules.plateforme.dto;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class ResultatLigneUtilisateurDto {
    private int numeroLigne;
    private String email;
    private boolean valide;
    @Builder.Default
    private List<String> erreurs = new ArrayList<>();
}
