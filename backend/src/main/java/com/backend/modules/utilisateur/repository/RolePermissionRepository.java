package com.backend.modules.utilisateur.repository;

import com.backend.modules.utilisateur.entity.RolePermission;
import com.backend.modules.utilisateur.entity.RolePermissionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {

    @Query("""
            select p.code
            from RolePermission rp
            join rp.role r
            join rp.permission p
            where r.code = :role
            """)
    List<String> listerCodesParRole(@Param("role") String role);
}
