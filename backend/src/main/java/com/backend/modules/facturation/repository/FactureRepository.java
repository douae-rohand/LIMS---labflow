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

    boolean existsByDemandeId(Long demandeId);

    @Query("""
            SELECT f FROM Facture f
            JOIN f.demande d
            LEFT JOIN d.client c
            WHERE (:statutPayee IS NULL OR
                   (CASE WHEN :statutPayee = true THEN f.statut = 'PAYEE' ELSE f.statut != 'PAYEE' END))
              AND (:echeanceDepassee IS NULL OR
                   (CASE WHEN :echeanceDepassee = true THEN f.dateEcheance < CURRENT_DATE ELSE true END))
              AND (:debut IS NULL OR f.dateFacture >= :debut)
              AND (:fin IS NULL OR f.dateFacture <= :fin)
              AND (:clientId IS NULL OR c.id = :clientId)
              AND (:q IS NULL OR :q = '' OR
                   LOWER(f.numero) LIKE LOWER(CONCAT('%', :q, '%')) ESCAPE '!' OR
                   LOWER(d.numero) LIKE LOWER(CONCAT('%', :q, '%')) ESCAPE '!' OR
                   LOWER(c.raisonSociale) LIKE LOWER(CONCAT('%', :q, '%')) ESCAPE '!')
            """)
    Page<Facture> rechercher(@Param("statutPayee") Boolean statutPayee,
                             @Param("echeanceDepassee") Boolean echeanceDepassee,
                             @Param("debut") java.time.LocalDate debut,
                             @Param("fin") java.time.LocalDate fin,
                             @Param("clientId") Long clientId,
                             @Param("q") String q,
                             Pageable pageable);

    @Query("SELECT SUM(f.montantHt) FROM Facture f WHERE f.dateFacture BETWEEN :debut AND :fin")
    java.math.BigDecimal sumMontantHtBetween(@Param("debut") java.time.LocalDate debut,
                                            @Param("fin") java.time.LocalDate fin);

    @Query("SELECT SUM(p.montant) FROM Paiement p WHERE p.datePaiement BETWEEN :debut AND :fin")
    java.math.BigDecimal sumMontantBetween(@Param("debut") java.time.LocalDate debut,
                                          @Param("fin") java.time.LocalDate fin);

    @Query("SELECT SUM(f.montantHt - COALESCE((SELECT SUM(p.montant) FROM Paiement p WHERE p.facture.id = f.id), 0)) FROM Facture f WHERE f.dateFacture BETWEEN :debut AND :fin")
    java.math.BigDecimal sumImpayeBetween(@Param("debut") java.time.LocalDate debut,
                                         @Param("fin") java.time.LocalDate fin);

    @Query("SELECT COUNT(f) FROM Facture f WHERE f.dateFacture BETWEEN :debut AND :fin AND f.statut != 'PAYEE'")
    Long countImpayeesBetween(@Param("debut") java.time.LocalDate debut,
                              @Param("fin") java.time.LocalDate fin);

    @Query("SELECT COUNT(f) FROM Facture f WHERE f.statut != 'PAYEE' AND f.dateEcheance < :date")
    Long countEnRetardBefore(@Param("date") java.time.LocalDate date);

    @Query("SELECT MAX(CAST(SUBSTRING(f.numero, 12, 3) AS integer)) FROM Facture f WHERE YEAR(f.dateFacture) = :annee AND MONTH(f.dateFacture) = :mois")
    Integer maxNumeroDuMois(@Param("annee") int annee, @Param("mois") int mois);

    @Query("SELECT SUM(f.montantHt) FROM Facture f WHERE YEAR(f.dateFacture) = :annee AND MONTH(f.dateFacture) = :mois")
    java.math.BigDecimal sumMontantHtByMonth(@Param("annee") int annee, @Param("mois") int mois);
}
