package com.backend.modules.plateforme.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResultatCompteAdministrateurDto {
    private boolean emailAccepte;
    private String message;
}
