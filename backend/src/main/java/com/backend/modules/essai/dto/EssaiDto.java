package com.backend.modules.essai.dto;

import com.backend.modules.essai.entity.StatutEssai;
import lombok.*;
import java.time.Instant;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class EssaiDto {
    private Long id;
    private Long echantillonId;
    private String analyseTypeCode;
    private Long technicienId;
    private StatutEssai statut;
    private String resultatValeur;
    private String resultatUnite;
    private Double valeurMin;
    private Double valeurMax;
    private Boolean conforme;
    private String observations;
    private Instant dateDebut;
    private Instant dateFin;
}
