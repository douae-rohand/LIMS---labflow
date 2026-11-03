package com.backend.modules.plateforme.dto;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class RapportImportUtilisateurDto {
    private int total;
    private int valides;
    private int invalides;
    private int enregistres;
    private int invitationsNonEnvoyees;
    /** Présent seulement après l'analyse. La confirmation n'accepte que ce jeton. */
    private String jetonAnalyse;
    /** Vrai tant qu'aucune ligne n'a été écrite. */
    private boolean apercu;
    private String message;
    @Builder.Default
    private List<ResultatLigneUtilisateurDto> lignes = new ArrayList<>();
    @Builder.Default
    private List<ResultatLigneUtilisateurDto> invitationsEchouees = new ArrayList<>();
}
