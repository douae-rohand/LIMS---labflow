package com.backend.modules.rapport.dto;

import com.backend.modules.rapport.entity.StatutRapport;
import lombok.*;
import java.time.Instant;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RapportDto {
    private Long id;
    private String reference;
    private Long demandeId;
    private StatutRapport statut;
    private String fichierUrl;
    private Long generateurId;
    private Long signataireId;
    private Instant dateGeneration;
    private Instant dateEnvoi;
}
