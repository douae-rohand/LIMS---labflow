package com.backend.modules.planification.repository;

import com.backend.modules.planification.entity.Planification;
import com.backend.modules.planification.entity.StatutPlanification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanificationRepository extends JpaRepository<Planification, Long> {
    List<Planification> findByDemandeId(Long demandeId);
    Page<Planification> findByTechnicienId(Long technicienId, Pageable pageable);
    Page<Planification> findByStatut(StatutPlanification statut, Pageable pageable);
}
