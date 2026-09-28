package com.backend.modules.validation.repository;

import com.backend.modules.validation.entity.Validation;
import com.backend.modules.validation.entity.StatutValidation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ValidationRepository extends JpaRepository<Validation, Long> {
    List<Validation> findByEssaiId(Long essaiId);
    Page<Validation> findByValidateurId(Long validateurId, Pageable pageable);
    Page<Validation> findByStatut(StatutValidation statut, Pageable pageable);
}
