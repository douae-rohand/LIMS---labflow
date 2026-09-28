package com.backend.modules.demande.repository;

import com.backend.modules.demande.entity.Demande;
import com.backend.modules.demande.entity.StatutDemande;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DemandeRepository extends JpaRepository<Demande, Long> {
    Optional<Demande> findByReference(String reference);
    Page<Demande> findByClientId(Long clientId, Pageable pageable);
    Page<Demande> findByStatut(StatutDemande statut, Pageable pageable);
}
