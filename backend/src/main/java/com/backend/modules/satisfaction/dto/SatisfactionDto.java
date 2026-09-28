package com.backend.modules.satisfaction.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.Instant;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SatisfactionDto {
    private Long id;
    private Long demandeId;
    private Long clientId;
    @Min(1) @Max(5)
    private Integer note;
    private String commentaire;
    @Min(1) @Max(5) private Integer noteDelai;
    @Min(1) @Max(5) private Integer noteQualite;
    @Min(1) @Max(5) private Integer noteCommunication;
    private Instant dateReponse;
}
