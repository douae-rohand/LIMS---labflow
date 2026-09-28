package com.backend.modules.catalogue.repository;

import com.backend.modules.catalogue.entity.AnalyseType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnalyseTypeRepository extends JpaRepository<AnalyseType, Long> {
    Optional<AnalyseType> findByCode(String code);
    boolean existsByCode(String code);
    Page<AnalyseType> findByActif(boolean actif, Pageable pageable);
}
