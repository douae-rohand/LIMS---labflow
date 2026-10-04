package com.backend.modules.produit.repository;

import com.backend.modules.produit.entity.Lot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LotRepository extends JpaRepository<Lot, Long> {

    List<Lot> findByProduit_Id(Long produitId);

    Optional<Lot> findFirstByProduit_IdOrderByPeremptionAsc(Long produitId);
}
