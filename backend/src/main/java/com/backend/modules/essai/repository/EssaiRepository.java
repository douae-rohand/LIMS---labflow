package com.backend.modules.essai.repository;

import com.backend.modules.essai.entity.Essai;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EssaiRepository extends JpaRepository<Essai, Long> {

    Optional<Essai> findByCode(String code);

    boolean existsByCode(String code);

    Page<Essai> findByActif(Boolean actif, Pageable pageable);

    Optional<Essai> findFirstByActifTrue();
}
