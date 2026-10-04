package com.backend.modules.essai.repository;

import com.backend.modules.essai.entity.LigneEssai;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LigneEssaiRepository extends JpaRepository<LigneEssai, Long> {

    List<LigneEssai> findByEchantillon_Id(Long echantillonId);

    List<LigneEssai> findByDemande_Id(Long demandeId);

    Page<LigneEssai> findByTechnicienId(Long technicienId, Pageable pageable);

    Page<LigneEssai> findByStatut(String statut, Pageable pageable);
}
