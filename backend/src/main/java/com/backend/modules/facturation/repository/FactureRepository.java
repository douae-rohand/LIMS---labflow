package com.backend.modules.facturation.repository;

import com.backend.modules.facturation.entity.Facture;
import com.backend.modules.facturation.entity.StatutFacture;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface FactureRepository extends JpaRepository<Facture, Long> {
    Optional<Facture> findByNumero(String numero);
    Page<Facture> findByClientId(Long clientId, Pageable pageable);
    Page<Facture> findByStatut(StatutFacture statut, Pageable pageable);
}
