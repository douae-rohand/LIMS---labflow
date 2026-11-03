package com.backend.modules.facturation.repository;

import com.backend.modules.facturation.entity.Facture;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FactureRepository extends JpaRepository<Facture, Long> {

    Optional<Facture> findByNumero(String numero);

    Page<Facture> findByDemande_Client_Id(Long clientId, Pageable pageable);

    Page<Facture> findByDemande_Client_UtilisateurId(Long utilisateurId, Pageable pageable);

    List<Facture> findByDemande_Client_UtilisateurId(Long utilisateurId);

    Page<Facture> findByStatut(String statut, Pageable pageable);

    /**
     * Factures d'un client pour un laboratoire donné, avec la demande (et son
     * client) chargées en une requête : évite le N+1 sur {@code demande.numero}
     * et {@code client.ice}. Trié par date de facture décroissante.
     */
    @Query("""
            SELECT f FROM Facture f
            JOIN FETCH f.demande d
            LEFT JOIN FETCH d.client
            WHERE d.client.utilisateurId = :utilisateurId
            ORDER BY f.dateFacture DESC
            """)
    List<Facture> findParUtilisateurAvecDemande(@Param("utilisateurId") Long utilisateurId);

    /** Facture d'un client pour un laboratoire donné (détail espace client). */
    @Query("""
            SELECT f FROM Facture f
            JOIN FETCH f.demande d
            LEFT JOIN FETCH d.client
            WHERE f.id = :factureId AND d.client.utilisateurId = :utilisateurId
            """)
    Optional<Facture> findVisibleClientParId(@Param("factureId") Long factureId,
                                             @Param("utilisateurId") Long utilisateurId);
}
