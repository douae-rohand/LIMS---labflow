package com.backend.modules.notification.repository;

import com.backend.modules.notification.entity.EvenementMetier;
import com.backend.modules.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByEvenement(EvenementMetier evenement, Pageable pageable);
    Page<Notification> findByEntiteSourceAndIdEntiteSource(String entiteSource, Long idEntiteSource, Pageable pageable);
}
