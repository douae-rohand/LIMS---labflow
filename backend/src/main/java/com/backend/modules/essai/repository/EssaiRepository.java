package com.backend.modules.essai.repository;

import com.backend.modules.essai.entity.Essai;
import com.backend.modules.essai.entity.StatutEssai;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EssaiRepository extends JpaRepository<Essai, Long> {
    List<Essai> findByEchantillonId(Long echantillonId);
    Page<Essai> findByTechnicienId(Long technicienId, Pageable pageable);
    Page<Essai> findByStatut(StatutEssai statut, Pageable pageable);
}
