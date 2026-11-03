package com.backend.modules.plateforme.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PermissionsCompteDto {
    private boolean personnalisable;
    private boolean personnalisees;
    private List<LignePermissionDto> permissions;
    private List<String> actionsNonDelegables;
}
