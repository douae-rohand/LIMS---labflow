package com.backend.modules.facturation.repository;

import com.backend.modules.facturation.entity.FactureLigne;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FactureLigneRepository extends JpaRepository<FactureLigne, Long> {

    List<FactureLigne> findByFacture_IdOrderByIdAsc(Long factureId);
}
