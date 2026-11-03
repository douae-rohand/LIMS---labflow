package com.backend.modules.satisfaction.repository;

import com.backend.modules.satisfaction.entity.EnqueteSatisfaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface EnqueteSatisfactionRepository extends JpaRepository<EnqueteSatisfaction, Long> {

    boolean existsByDemande_Id(Long demandeId);

    @Query("SELECT AVG(e.noteGlobale) FROM EnqueteSatisfaction e")
    Double calculerMoyenneGlobale();

    /**
     * Enquêtes d'un client pour un laboratoire donné, avec la demande chargée en
     * une requête (évite le N+1). Tri appliqué côté service.
     */
    @Query("""
            SELECT e FROM EnqueteSatisfaction e
            JOIN FETCH e.demande d
            WHERE d.client.utilisateurId = :utilisateurId
            """)
    List<EnqueteSatisfaction> findParUtilisateurAvecDemande(@Param("utilisateurId") Long utilisateurId);

    /** Enquête d'un client pour un laboratoire donné (détail espace client). */
    @Query("""
            SELECT e FROM EnqueteSatisfaction e
            JOIN FETCH e.demande d
            WHERE e.id = :enqueteId AND d.client.utilisateurId = :utilisateurId
            """)
    Optional<EnqueteSatisfaction> findVisibleClientParId(@Param("enqueteId") Long enqueteId,
                                                         @Param("utilisateurId") Long utilisateurId);

    /**
     * Réponse atomique et unique : ne met à jour que si l'enquête n'a pas encore
     * été répondue ({@code date_reponse IS NULL}). Le nombre de lignes renvoyé
     * permet de détecter une double soumission (0 ligne).
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE EnqueteSatisfaction e
            SET e.dateReponse = :dateReponse,
                e.noteGlobale = :noteGlobale,
                e.noteDelai = :noteDelai,
                e.noteClarte = :noteClarte,
                e.noteRelation = :noteRelation,
                e.commentaire = :commentaire
            WHERE e.id = :enqueteId AND e.dateReponse IS NULL
            """)
    int repondreSiNonRepondue(@Param("enqueteId") Long enqueteId,
                              @Param("dateReponse") Instant dateReponse,
                              @Param("noteGlobale") Integer noteGlobale,
                              @Param("noteDelai") Integer noteDelai,
                              @Param("noteClarte") Integer noteClarte,
                              @Param("noteRelation") Integer noteRelation,
                              @Param("commentaire") String commentaire);

    @Query("SELECT COUNT(e) FROM EnqueteSatisfaction e WHERE e.dateEnvoi BETWEEN :debut AND :fin")
    Long countEnvoyeesBetween(@Param("debut") Instant debut, @Param("fin") Instant fin);

    @Query("SELECT COUNT(e) FROM EnqueteSatisfaction e WHERE e.dateReponse IS NOT NULL AND e.dateReponse BETWEEN :debut AND :fin")
    Long countReponduesBetween(@Param("debut") Instant debut, @Param("fin") Instant fin);

    @Query("SELECT AVG(e.noteGlobale) FROM EnqueteSatisfaction e WHERE e.dateReponse IS NOT NULL")
    Double moyenneGlobaleRepondues();

    @Query("SELECT AVG(e.noteDelai) FROM EnqueteSatisfaction e WHERE e.dateReponse IS NOT NULL")
    Double moyenneDelai();

    @Query("SELECT AVG(e.noteClarte) FROM EnqueteSatisfaction e WHERE e.dateReponse IS NOT NULL")
    Double moyenneClarte();

    @Query("SELECT AVG(e.noteRelation) FROM EnqueteSatisfaction e WHERE e.dateReponse IS NOT NULL")
    Double moyenneRelation();

    @Query("SELECT e.noteGlobale, COUNT(e) FROM EnqueteSatisfaction e WHERE e.dateReponse IS NOT NULL GROUP BY e.noteGlobale")
    List<Object[]> countByNoteGlobale();

    @Query("SELECT YEAR(e.dateReponse), MONTH(e.dateReponse), AVG(e.noteGlobale) FROM EnqueteSatisfaction e WHERE e.dateReponse IS NOT NULL GROUP BY YEAR(e.dateReponse), MONTH(e.dateReponse) ORDER BY YEAR(e.dateReponse), MONTH(e.dateReponse)")
    List<Object[]> moyenneByMonth();

    @Query("SELECT e.id, e.code, e.dateEnvoi, e.dateReponse, e.noteGlobale, e.noteDelai, e.noteClarte, e.noteRelation, e.commentaire FROM EnqueteSatisfaction e WHERE e.dateReponse IS NOT NULL ORDER BY e.dateReponse DESC")
    org.springframework.data.domain.Page<Object[]> listerPourExport(org.springframework.data.domain.Pageable pageable);
}
