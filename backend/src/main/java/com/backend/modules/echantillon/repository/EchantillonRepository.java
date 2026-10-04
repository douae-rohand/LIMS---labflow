package com.backend.modules.echantillon.repository;

import com.backend.modules.echantillon.entity.Echantillon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EchantillonRepository extends JpaRepository<Echantillon, Long> {

    Optional<Echantillon> findByReference(String reference);

    Page<Echantillon> findByDemande_Id(Long demandeId, Pageable pageable);

    Page<Echantillon> findByConformite(Boolean conformite, Pageable pageable);
}
