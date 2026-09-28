package com.backend.modules.plateforme.dto;

import com.backend.modules.plateforme.entity.StatutIntegration;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DemandeIntegrationDto {
    private Long id;

    @NotBlank @Size(max = 200)
    private String nomLaboratoire;

    @NotBlank @Email @Size(max = 180)
    private String emailRepresentant;

    @Size(max = 200)
    private String nomRepresentant;

    @Size(max = 20)
    private String telephoneRepresentant;

    private String message;

    private StatutIntegration statut;
    private String commentaireAdmin;
    private Instant dateSoumission;
    private Instant dateTraitement;
}
