package com.backend.modules.catalogue.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModifierDomaineRequest {
    @Size(max = 100, message = "Le libellé ne peut pas dépasser 100 caractères")
    private String libelle;
}
