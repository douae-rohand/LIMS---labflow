package com.backend.modules.plateforme.repository;

import com.backend.modules.plateforme.entity.Laboratoire;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LaboratoireRepository extends JpaRepository<Laboratoire, Long> {

    Optional<Laboratoire> findByCode(String code);

    Optional<Laboratoire> findByNomSchema(String nomSchema);

    boolean existsByCode(String code);

    Page<Laboratoire> findByStatut(String statut, Pageable pageable);
}
