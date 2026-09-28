package com.backend.modules.conformite.repository;

import com.backend.modules.conformite.entity.NonConformite;
import com.backend.modules.conformite.entity.StatutNonConformite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NonConformiteRepository extends JpaRepository<NonConformite, Long> {
    Page<NonConformite> findByStatut(StatutNonConformite statut, Pageable pageable);
    Page<NonConformite> findByDeclarantId(Long declarantId, Pageable pageable);
}
