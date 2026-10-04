package com.backend.modules.facturation.repository;

import com.backend.modules.facturation.entity.Facture;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FactureRepository extends JpaRepository<Facture, Long> {

    Optional<Facture> findByNumero(String numero);

    Page<Facture> findByDemande_Client_Id(Long clientId, Pageable pageable);

    Page<Facture> findByDemande_Client_UtilisateurId(Long utilisateurId, Pageable pageable);

    java.util.List<Facture> findByDemande_Client_UtilisateurId(Long utilisateurId);

    Page<Facture> findByStatut(String statut, Pageable pageable);
}
