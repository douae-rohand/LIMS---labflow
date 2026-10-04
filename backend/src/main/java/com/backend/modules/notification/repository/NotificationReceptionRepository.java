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
}
