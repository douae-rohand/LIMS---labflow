package com.backend.modules.plateforme.repository;

import com.backend.modules.plateforme.entity.DemandeIntegration;
import com.backend.modules.plateforme.entity.StatutIntegration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DemandeIntegrationRepository extends JpaRepository<DemandeIntegration, Long> {

    Page<DemandeIntegration> findByStatut(StatutIntegration statut, Pageable pageable);

    boolean existsByEmailRepresentantAndStatut(String email, StatutIntegration statut);
}
