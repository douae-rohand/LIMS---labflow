package com.backend.modules.plateforme.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LignePermissionDto {
    private String code;
    private String module;
    private String action;
    private String libelle;
    private boolean parDefaut;
    private boolean deleguable;
    /** AJOUT, RETRAIT, ou null lorsque le compte suit le défaut du rôle. */
    private String ecart;
    private boolean effective;
}
