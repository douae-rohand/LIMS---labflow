package com.backend.modules.validation.dto;

import com.backend.modules.validation.entity.StatutValidation;
import lombok.*;
import java.time.Instant;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ValidationDto {
    private Long id;
    private Long essaiId;
    private Long validateurId;
    private StatutValidation statut;
    private String commentaire;
    private Instant dateValidation;
}
