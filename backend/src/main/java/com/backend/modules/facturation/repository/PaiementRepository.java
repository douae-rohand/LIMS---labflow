package com.backend.modules.facturation.repository;

import com.backend.modules.facturation.entity.Paiement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface PaiementRepository extends JpaRepository<Paiement, Long> {

    /** Paiements de plusieurs factures en une requête (évite le N+1). */
    List<Paiement> findByFacture_IdIn(Collection<Long> factureIds);

    List<Paiement> findByFactureId(Long factureId);

    @Query("SELECT SUM(p.montant) FROM Paiement p WHERE p.facture.id = :factureId")
    java.math.BigDecimal sumByFactureId(@Param("factureId") Long factureId);

    @Query("SELECT SUM(p.montant) FROM Paiement p WHERE YEAR(p.datePaiement) = :annee AND MONTH(p.datePaiement) = :mois")
    java.math.BigDecimal sumMontantByMonth(@Param("annee") int annee, @Param("mois") int mois);

    @Query("SELECT SUM(p.montant) FROM Paiement p WHERE p.datePaiement BETWEEN :debut AND :fin")
    java.math.BigDecimal sumMontantBetween(@Param("debut") java.time.Instant debut, @Param("fin") java.time.Instant fin);
}
