package com.backend.modules.ia.dto;

import lombok.*;
import java.time.Instant;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AnalyseIaResponse {
    private TypeTacheIa typeTache;
    private String resultat;
    private Double scoreConfiance;
    private String modeleUtilise;
    private Instant dateAnalyse;
}
