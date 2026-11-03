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
}
