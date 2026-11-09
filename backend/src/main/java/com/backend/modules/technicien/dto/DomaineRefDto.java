package com.backend.modules.technicien.dto;

import lombok.Builder;
import lombok.Value;

/** Référence légère à un domaine d'analyse (pas d'identifiant exposé). */
@Value
@Builder
public class DomaineRefDto {
    String code;
    String libelle;
}
