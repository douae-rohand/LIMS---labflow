package com.backend.common.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface JournalAuditRepository extends JpaRepository<JournalAudit, Long> {

    Page<JournalAudit> findByUtilisateur_Email(String email, Pageable pageable);

    Page<JournalAudit> findByObjet(String objet, Pageable pageable);

    List<JournalAudit> findByDateActionBetween(Instant debut, Instant fin);

    Page<JournalAudit> findByAction(String action, Pageable pageable);

    @Query("SELECT DISTINCT j.action FROM JournalAudit j WHERE j.action IS NOT NULL ORDER BY j.action")
    List<String> listerActions();

    @Query(
            value = """
                    SELECT j FROM JournalAudit j
                    JOIN FETCH j.utilisateur u
                    WHERE (:acteur IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :acteur, '%')) ESCAPE '\\')
                      AND (:action IS NULL OR j.action = :action)
                      AND (:laboratoireId IS NULL OR j.objet = :objetLaboratoire OR j.laboratoire.id = :laboratoireId)
                      AND (:debut IS NULL OR j.dateAction >= :debut)
                      AND (:fin IS NULL OR j.dateAction < :fin)
                    """,
            countQuery = """
                    SELECT COUNT(j) FROM JournalAudit j
                    JOIN j.utilisateur u
                    WHERE (:acteur IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :acteur, '%')) ESCAPE '\\')
                      AND (:action IS NULL OR j.action = :action)
                      AND (:laboratoireId IS NULL OR j.objet = :objetLaboratoire OR j.laboratoire.id = :laboratoireId)
                      AND (:debut IS NULL OR j.dateAction >= :debut)
                      AND (:fin IS NULL OR j.dateAction < :fin)
                    """)
    Page<JournalAudit> rechercher(
            @Param("acteur") String acteur,
            @Param("action") String action,
            @Param("objetLaboratoire") String objetLaboratoire,
            @Param("laboratoireId") Long laboratoireId,
            @Param("debut") Instant debut,
            @Param("fin") Instant fin,
            Pageable pageable);
}
