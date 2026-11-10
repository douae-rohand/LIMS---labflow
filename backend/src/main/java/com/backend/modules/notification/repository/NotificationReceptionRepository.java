package com.backend.modules.notification.repository;

import com.backend.modules.notification.entity.NotificationReception;
import com.backend.modules.notification.entity.NotificationReceptionId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationReceptionRepository extends JpaRepository<NotificationReception, NotificationReceptionId> {

    Page<NotificationReception> findByUtilisateur_Id(Long utilisateurId, Pageable pageable);

    Page<NotificationReception> findByUtilisateur_IdAndLueFalse(Long utilisateurId, Pageable pageable);

    long countByUtilisateur_IdAndLueFalse(Long utilisateurId);

    @org.springframework.data.jpa.repository.Query("""
            SELECT r FROM NotificationReception r JOIN FETCH r.notification n
            WHERE r.utilisateur.id = :utilisateurId
            ORDER BY n.dateCreation DESC
            """)
    java.util.List<NotificationReception> listerRecentes(
            @org.springframework.data.repository.query.Param("utilisateurId") Long utilisateurId,
            Pageable pageable);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("""
            UPDATE NotificationReception r SET r.lue = true, r.dateLecture = :date
            WHERE r.utilisateur.id = :utilisateurId AND r.lue = false
            """)
    int marquerToutesLues(@org.springframework.data.repository.query.Param("utilisateurId") Long utilisateurId,
                          @org.springframework.data.repository.query.Param("date") java.time.Instant date);
}
