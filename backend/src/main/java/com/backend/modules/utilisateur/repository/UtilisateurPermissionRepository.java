package com.backend.modules.utilisateur.repository;

import com.backend.modules.utilisateur.entity.UtilisateurPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface UtilisateurPermissionRepository extends JpaRepository<UtilisateurPermission, Long> {

    @Query("""
            select up.effet, p.code
            from UtilisateurPermission up, Permission p
            where up.permissionId = p.id and up.utilisateurId = :utilisateurId
            """)
    List<Object[]> listerEcarts(Long utilisateurId);

    List<UtilisateurPermission> findByUtilisateurId(Long utilisateurId);

    @Query("select distinct up.utilisateurId from UtilisateurPermission up where up.utilisateurId in :ids")
    List<Long> identifiantsPersonnalises(@Param("ids") Collection<Long> ids);

    @Modifying
    @Query("delete from UtilisateurPermission up where up.utilisateurId = :utilisateurId")
    void deleteByUtilisateurId(Long utilisateurId);

    @Modifying
    @Query("""
            delete from UtilisateurPermission up
            where up.utilisateurId = :utilisateurId
              and up.permissionId in :permissionIds
            """)
    void deleteByUtilisateurIdAndPermissionIdIn(
            @Param("utilisateurId") Long utilisateurId,
            @Param("permissionIds") Collection<Long> permissionIds);
}
