package com.backend.modules.plateforme.dto;

import com.backend.modules.plateforme.entity.TypeDocumentIntegration;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentIntegrationDto {
    private Long id;
    private TypeDocumentIntegration typeDocument;
    private String libelle;
    private String raison;
    private String nomFichier;
    private String typeMime;
    private Long taille;
    private Instant dateAjout;
}
