package com.backend.modules.plateforme.repository;

import com.backend.modules.plateforme.entity.DemandeIntegration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DemandeIntegrationRepository extends JpaRepository<DemandeIntegration, Long> {

    Page<DemandeIntegration> findByStatut(String statut, Pageable pageable);

    boolean existsByContactEmailAndStatut(String contactEmail, String statut);

    boolean existsByIceAndStatut(String ice, String statut);
}
