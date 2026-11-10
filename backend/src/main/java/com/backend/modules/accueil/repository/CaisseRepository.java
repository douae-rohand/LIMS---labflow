package com.backend.modules.accueil.repository;

import com.backend.modules.facturation.entity.Facture;
import com.backend.modules.facturation.entity.Paiement;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Requêtes de la caisse de l'accueil : factures, paiements et numérotation des reçus. */
public interface CaisseRepository extends Repository<Paiement, Long> {

    Paiement save(Paiement paiement);

    /** Factures du laboratoire avec leur demande et leur client, les plus récentes d'abord. */
    @Query("""
            SELECT f FROM Facture f
            JOIN FETCH f.demande d
            LEFT JOIN FETCH d.client
            ORDER BY f.dateFacture DESC, f.id DESC
            """)
    List<Facture> factures();

    @Query("""
            SELECT f FROM Facture f
            JOIN FETCH f.demande d
            LEFT JOIN FETCH d.client
            WHERE f.id = :id
            """)
    Optional<Facture> facture(@Param("id") Long id);

    @Query("SELECT f FROM Facture f WHERE f.demande.id = :demandeId ORDER BY f.id DESC")
    List<Facture> facturesDeLaDemande(@Param("demandeId") Long demandeId);

    @Query("SELECT p FROM Paiement p WHERE p.facture.id IN :factureIds ORDER BY p.datePaiement DESC, p.id DESC")
    List<Paiement> paiementsDes(@Param("factureIds") Collection<Long> factureIds);

    @Query("""
            SELECT p FROM Paiement p
            JOIN FETCH p.facture f
            JOIN FETCH f.demande d
            LEFT JOIN FETCH d.client
            WHERE p.id = :id
            """)
    Optional<Paiement> paiement(@Param("id") Long id);

    /** Plus grand numéro de reçu attribué pour un préfixe PAY-AAAAMM- (longueur 11). */
    @Query("SELECT MAX(CAST(SUBSTRING(p.code, 12) AS Integer)) FROM Paiement p WHERE p.code LIKE :prefixe")
    Integer dernierNumero(@Param("prefixe") String prefixe);
}
