package com.backend.modules.rapport.repository;

import com.backend.modules.rapport.entity.Rapport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RapportRepository extends JpaRepository<Rapport, Long> {

    Optional<Rapport> findByNumero(String numero);

    Page<Rapport> findByDemande_Id(Long demandeId, Pageable pageable);

    Page<Rapport> findByStatut(String statut, Pageable pageable);
}
