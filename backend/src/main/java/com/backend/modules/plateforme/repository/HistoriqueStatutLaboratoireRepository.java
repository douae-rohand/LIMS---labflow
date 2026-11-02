package com.backend.modules.plateforme.repository;

import com.backend.modules.plateforme.entity.HistoriqueStatutLaboratoire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistoriqueStatutLaboratoireRepository extends JpaRepository<HistoriqueStatutLaboratoire, Long> {
    List<HistoriqueStatutLaboratoire> findByLaboratoire_IdOrderByDateChangementDesc(Long laboratoireId);
}
