package com.backend.modules.rapport.repository;

import com.backend.modules.rapport.entity.Rapport;
import com.backend.modules.rapport.entity.StatutRapport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RapportRepository extends JpaRepository<Rapport, Long> {
    Optional<Rapport> findByReference(String reference);
    Page<Rapport> findByDemandeId(Long demandeId, Pageable pageable);
    Page<Rapport> findByStatut(StatutRapport statut, Pageable pageable);
}
