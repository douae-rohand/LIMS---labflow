package com.backend.modules.ia.repository;

import com.backend.modules.ia.entity.SyntheseIa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SyntheseIaRepository extends JpaRepository<SyntheseIa, Long> {

    /** Dernière synthèse générée pour une demande, si elle existe. */
    Optional<SyntheseIa> findFirstByDemande_IdOrderByDateGenerationDesc(Long demandeId);
}
