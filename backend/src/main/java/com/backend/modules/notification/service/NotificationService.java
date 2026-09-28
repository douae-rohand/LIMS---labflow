package com.backend.modules.notification.service;

import com.backend.common.exception.ResourceNotFoundException;
import com.backend.modules.notification.dto.NotificationDto;
import com.backend.modules.notification.entity.*;
import com.backend.modules.notification.repository.NotificationDestinataireRepository;
import com.backend.modules.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Service central de notification.
 *
 * <p>Flux :
 * <ol>
 *   <li>Un événement métier est émis (ex. depuis {@link com.backend.integration.n8n.NotificationEventPublisher}).</li>
 *   <li>{@code publier()} persiste la {@link Notification} et ses {@link NotificationDestinataire}.</li>
 *   <li>Les canaux IN_APP sont livrés immédiatement via WebSocket (STOMP).</li>
 *   <li>Les canaux EMAIL / WEB_PUSH / N8N_WEBHOOK sont délégués aux services d'intégration.</li>
 * </ol>
 *
 * <p>TODO: remplacer l'envoi synchrone par un bus d'événements (Spring Events / Kafka).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationDestinataireRepository destinataireRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // -------------------------------------------------------------------------
    // Publication
    // -------------------------------------------------------------------------

    /**
     * Persiste et distribue une notification à ses destinataires.
     */
    @Transactional
    public NotificationDto publier(NotificationDto dto) {
        // 1. Persister la notification
        Notification notif = Notification.builder()
                .evenement(dto.getEvenement())
                .titre(dto.getTitre())
                .corps(dto.getCorps())
                .payloadJson(dto.getPayloadJson())
                .entiteSource(dto.getEntiteSource())
                .idEntiteSource(dto.getIdEntiteSource())
                .build();
        notif = notificationRepository.save(notif);

        // 2. Créer les entrées destinataire
        List<CanalNotification> canaux = dto.getCanaux() != null
                ? dto.getCanaux()
                : List.of(CanalNotification.IN_APP);

        if (dto.getDestinataireIds() != null) {
            for (Long uid : dto.getDestinataireIds()) {
                for (CanalNotification canal : canaux) {
                    NotificationDestinataire dest = NotificationDestinataire.builder()
                            .notification(notif)
                            .utilisateurId(uid)
                            .canal(canal)
                            .build();
                    destinataireRepository.save(dest);

                    // Livraison WebSocket immédiate pour IN_APP
                    if (canal == CanalNotification.IN_APP) {
                        livrerInApp(uid, notif);
                    }
                    // TODO: EMAIL → emailService.envoyer(...)
                    // TODO: WEB_PUSH → webPushService.envoyer(...)
                    // TODO: N8N_WEBHOOK → notificationEventPublisher.publier(...)
                }
            }
        }

        notif.setStatut(StatutNotification.ENVOYEE);
        notif.setDateEnvoi(Instant.now());
        notificationRepository.save(notif);

        return toDto(notif);
    }

    // -------------------------------------------------------------------------
    // Consultation (côté utilisateur)
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<NotificationDestinataire> mesNotifications(Long utilisateurId, Pageable pageable) {
        return destinataireRepository.findByUtilisateurId(utilisateurId, pageable);
    }

    @Transactional(readOnly = true)
    public long compterNonLues(Long utilisateurId) {
        return destinataireRepository.countByUtilisateurIdAndLuFalse(utilisateurId);
    }

    @Transactional
    public void marquerCommeLue(Long destinataireId) {
        NotificationDestinataire dest = destinataireRepository.findById(destinataireId)
                .orElseThrow(() -> new ResourceNotFoundException("NotificationDestinataire", "id", destinataireId));
        dest.setLu(true);
        dest.setDateLecture(Instant.now());
        destinataireRepository.save(dest);
    }

    @Transactional
    public void marquerToutesCommeLues(Long utilisateurId) {
        destinataireRepository.findNonLues(utilisateurId, Pageable.unpaged()).forEach(dest -> {
            dest.setLu(true);
            dest.setDateLecture(Instant.now());
            destinataireRepository.save(dest);
        });
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void livrerInApp(Long utilisateurId, Notification notif) {
        try {
            messagingTemplate.convertAndSendToUser(
                    utilisateurId.toString(),
                    "/queue/notifications",
                    toDto(notif));
        } catch (Exception ex) {
            log.error("Erreur livraison WebSocket pour userId={} : {}", utilisateurId, ex.getMessage());
        }
    }

    private NotificationDto toDto(Notification n) {
        return NotificationDto.builder()
                .id(n.getId()).evenement(n.getEvenement()).titre(n.getTitre())
                .corps(n.getCorps()).payloadJson(n.getPayloadJson())
                .entiteSource(n.getEntiteSource()).idEntiteSource(n.getIdEntiteSource())
                .statut(n.getStatut()).dateCreation(n.getDateCreation()).dateEnvoi(n.getDateEnvoi())
                .build();
    }
}
