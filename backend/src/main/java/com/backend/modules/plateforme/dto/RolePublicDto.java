package com.backend.modules.plateforme.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Rôle exposé publiquement : uniquement les colonnes persistées {@code role.code} et {@code role.libelle}.
 * Aucune capacité ni flag 2FA n'est inventé — ces données n'existent pas en base.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolePublicDto {
    private Long id;
    private String code;
    private String libelle;
}
