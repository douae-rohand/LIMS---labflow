package com.backend.modules.plateforme.dto;

import com.backend.modules.plateforme.entity.TypeDocumentIntegration;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TypeDocumentIntegrationDto {
    private String code;
    private String libelle;
    private String raison;
    private boolean obligatoire;
    private boolean actif;
    private int ordre;

    public static TypeDocumentIntegrationDto from(TypeDocumentIntegration type) {
        return TypeDocumentIntegrationDto.builder()
                .code(type.getCode())
                .libelle(type.getLibelle())
                .raison(type.getRaison())
                .obligatoire(type.isObligatoire())
                .actif(type.isActif())
                .ordre(type.getOrdre())
                .build();
    }
}
