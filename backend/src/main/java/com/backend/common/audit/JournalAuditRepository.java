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
                    JOIN u.role r
                    WHERE (:acteur IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :acteur, '%')) ESCAPE '\\'
                           OR LOWER(CONCAT(u.prenom, ' ', u.nom)) LIKE LOWER(CONCAT('%', :acteur, '%')) ESCAPE '\\')
                      AND (:role IS NULL OR r.code = :role)
                      AND (:action IS NULL OR j.action = :action)
                      AND (:typeObjet IS NULL OR j.objet LIKE CONCAT(:typeObjet, '#%'))
                      AND (:laboratoireId IS NULL OR j.objet LIKE CONCAT(:nomSchema, '/%') OR j.laboratoire.id = :laboratoireId)
                      AND (:debut IS NULL OR j.dateAction >= :debut)
                      AND (:fin IS NULL OR j.dateAction < :fin)
                      AND (:q IS NULL OR :q = '' OR LOWER(j.objet) LIKE LOWER(CONCAT('%', :q, '%')) ESCAPE '\\')
                    """,
            countQuery = """
                    SELECT COUNT(j) FROM JournalAudit j
                    JOIN j.utilisateur u
                    JOIN u.role r
                    WHERE (:acteur IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :acteur, '%')) ESCAPE '\\'
                           OR LOWER(CONCAT(u.prenom, ' ', u.nom)) LIKE LOWER(CONCAT('%', :acteur, '%')) ESCAPE '\\')
                      AND (:role IS NULL OR r.code = :role)
                      AND (:action IS NULL OR j.action = :action)
                      AND (:typeObjet IS NULL OR j.objet LIKE CONCAT(:typeObjet, '#%'))
                      AND (:laboratoireId IS NULL OR j.objet LIKE CONCAT(:nomSchema, '/%') OR j.laboratoire.id = :laboratoireId)
                      AND (:debut IS NULL OR j.dateAction >= :debut)
                      AND (:fin IS NULL OR j.dateAction < :fin)
                      AND (:q IS NULL OR :q = '' OR LOWER(j.objet) LIKE LOWER(CONCAT('%', :q, '%')) ESCAPE '\\')
                    """)
    Page<JournalAudit> rechercher(
            @Param("acteur") String acteur,
            @Param("role") String role,
            @Param("action") String action,
            @Param("typeObjet") String typeObjet,
            @Param("nomSchema") String nomSchema,
            @Param("laboratoireId") String laboratoireId,
            @Param("debut") Instant debut,
            @Param("fin") Instant fin,
            @Param("q") String q,
            Pageable pageable);
}
