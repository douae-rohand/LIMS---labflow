package com.backend.modules.notification.repository;

import com.backend.modules.notification.entity.NotificationDestinataire;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationDestinataireRepository extends JpaRepository<NotificationDestinataire, Long> {

    Page<NotificationDestinataire> findByUtilisateurId(Long utilisateurId, Pageable pageable);

    @Query("SELECT nd FROM NotificationDestinataire nd WHERE nd.utilisateurId = :uid AND nd.lu = false")
    Page<NotificationDestinataire> findNonLues(@Param("uid") Long utilisateurId, Pageable pageable);

    long countByUtilisateurIdAndLuFalse(Long utilisateurId);
}
