package com.backend.modules.echantillon.repository;

import com.backend.modules.echantillon.entity.Echantillon;
import com.backend.modules.echantillon.entity.StatutEchantillon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EchantillonRepository extends JpaRepository<Echantillon, Long> {
    Optional<Echantillon> findByCodeBarre(String codeBarre);
    Page<Echantillon> findByDemandeId(Long demandeId, Pageable pageable);
    Page<Echantillon> findByStatut(StatutEchantillon statut, Pageable pageable);
}
