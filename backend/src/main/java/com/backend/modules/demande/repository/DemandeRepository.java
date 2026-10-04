package com.backend.modules.demande.repository;

import com.backend.modules.demande.entity.Demande;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DemandeRepository extends JpaRepository<Demande, Long> {

    Optional<Demande> findByNumero(String numero);

    Page<Demande> findByClient_Id(Long clientId, Pageable pageable);

    Page<Demande> findByClient_UtilisateurId(Long utilisateurId, Pageable pageable);

    java.util.List<Demande> findByClient_UtilisateurId(Long utilisateurId);

    Page<Demande> findByStatut(String statut, Pageable pageable);
}
