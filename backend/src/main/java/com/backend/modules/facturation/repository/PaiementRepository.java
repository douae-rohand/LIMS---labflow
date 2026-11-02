package com.backend.modules.facturation.repository;

import com.backend.modules.facturation.entity.Paiement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface PaiementRepository extends JpaRepository<Paiement, Long> {

    /** Paiements de plusieurs factures en une requête (évite le N+1). */
    List<Paiement> findByFacture_IdIn(Collection<Long> factureIds);
}
