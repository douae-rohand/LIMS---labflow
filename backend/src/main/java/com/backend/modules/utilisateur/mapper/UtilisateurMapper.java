package com.backend.modules.utilisateur.mapper;

import com.backend.modules.utilisateur.dto.UtilisateurDto;
import com.backend.modules.utilisateur.entity.Utilisateur;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper MapStruct : {@link Utilisateur} ↔ {@link UtilisateurDto}.
 *
 * <p>Le composant Spring est généré automatiquement par le processeur
 * d'annotations MapStruct (componentModel = "spring" via compilerArg).
 */
@Mapper
public interface UtilisateurMapper {

    @Mapping(target = "nomComplet", expression = "java(utilisateur.getNomComplet())")
    @Mapping(target = "role", expression = "java(utilisateur.getRole() == null ? null : com.backend.modules.utilisateur.entity.RoleUtilisateur.valueOf(utilisateur.getRole().getCode()))")
    @Mapping(target = "deuxFacteursActif", source = "doubleAuthentification")
    @Mapping(target = "compteConfirme", source = "compteConfirme")
    @Mapping(target = "dateCreation", ignore = true)
    UtilisateurDto toDto(Utilisateur utilisateur);
}
