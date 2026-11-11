package com.backend.modules.accueil.repository;

import com.backend.modules.echantillon.entity.Echantillon;
import com.backend.modules.essai.entity.LigneEssai;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

/**
 * Requêtes de l'espace accueil (réception des échantillons), chargées en une fois pour
 * éviter les requêtes en cascade.
 */
public interface ReceptionRepository extends Repository<Echantillon, Long> {

    Echantillon save(Echantillon echantillon);

    /** Analyses des demandes dans ces statuts, avec demande, client, patient, essai, domaine et échantillon. */
    @Query("""
            SELECT le FROM LigneEssai le
            JOIN FETCH le.demande d
            LEFT JOIN FETCH d.client
            LEFT JOIN FETCH d.patient
            LEFT JOIN FETCH le.essai e
            LEFT JOIN FETCH e.domaine
            LEFT JOIN FETCH le.echantillon
            WHERE d.statut IN :statuts
            ORDER BY d.id DESC, le.id ASC
            """)
    List<LigneEssai> lignesDesDemandes(@Param("statuts") Collection<String> statuts);

    /** Analyses d'une demande, mêmes jointures. */
    @Query("""
            SELECT le FROM LigneEssai le
            JOIN FETCH le.demande d
            LEFT JOIN FETCH d.client
            LEFT JOIN FETCH d.patient
            LEFT JOIN FETCH le.essai e
            LEFT JOIN FETCH e.domaine
            LEFT JOIN FETCH le.echantillon
            WHERE d.id = :demandeId
            ORDER BY le.id ASC
            """)
    List<LigneEssai> lignesDeLaDemande(@Param("demandeId") Long demandeId);

    @Query("SELECT e FROM Echantillon e WHERE e.demande.id = :demandeId ORDER BY e.dateReception DESC, e.id DESC")
    List<Echantillon> echantillonsDeLaDemande(@Param("demandeId") Long demandeId);

    /** Date de la dernière réception par demande. */
    @Query("SELECT e.demande.id, MAX(e.dateReception) FROM Echantillon e WHERE e.demande.id IN :demandeIds GROUP BY e.demande.id")
    List<Object[]> dernieresReceptions(@Param("demandeIds") Collection<Long> demandeIds);

    /** Plus grand numéro déjà attribué pour un préfixe ECH-AAAAMM- (longueur 11). */
    @Query("SELECT MAX(CAST(SUBSTRING(e.reference, 12) AS Integer)) FROM Echantillon e WHERE e.reference LIKE :prefixe")
    Integer dernierNumero(@Param("prefixe") String prefixe);

    @Query("""
            SELECT e FROM Echantillon e JOIN FETCH e.demande
            WHERE e.dateReception >= :depuis
            ORDER BY e.dateReception DESC, e.id DESC
            """)
    List<Echantillon> recusDepuis(@Param("depuis") Instant depuis);

    /** Factures non soldées : nombre et reste à encaisser (montant moins paiements). */
    @Query("""
            SELECT COUNT(f), COALESCE(SUM(f.montantHt - COALESCE(
                (SELECT SUM(p.montant) FROM Paiement p WHERE p.facture = f), 0)), 0)
            FROM Facture f WHERE f.statut NOT IN ('PAYEE', 'ANNULEE')
            """)
    List<Object[]> facturesAEncaisser();
}
