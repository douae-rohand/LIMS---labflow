package com.backend.modules.produit.repository;

import com.backend.modules.produit.entity.Lot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LotRepository extends JpaRepository<Lot, Long> {

    List<Lot> findByProduit_Id(Long produitId);

    Optional<Lot> findFirstByProduit_IdOrderByPeremptionAsc(Long produitId);

    boolean existsByProduitId(Long produitId);

    boolean existsByProduitIdAndNumero(Long produitId, String numero);

    @Query("SELECT COALESCE(SUM(l.quantite), 0) FROM Lot l WHERE l.produit.id = :produitId AND l.statut = 'DISPONIBLE' AND l.peremption > CURRENT_DATE")
    java.math.BigDecimal sumQuantiteByProduitId(@Param("produitId") Long produitId);

    @Query("SELECT l FROM Lot l WHERE l.statut = 'PERIME'")
    List<Lot> findLotsPerimes();

    @Query("SELECT l FROM Lot l WHERE l.statut = 'DISPONIBLE' AND l.peremption BETWEEN CURRENT_DATE AND :date")
    List<Lot> findLotsPeremptionProche(@Param("date") LocalDate date);

    @Query("SELECT l FROM Lot l WHERE l.produit.id = :produitId")
    List<Lot> findByProduitId(@Param("produitId") Long produitId);
}
