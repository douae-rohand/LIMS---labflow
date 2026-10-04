package com.backend.modules.notification.service;

import com.backend.common.exception.ResourceNotFoundException;
import com.backend.modules.notification.dto.NotificationDto;
import com.backend.modules.notification.entity.CanalNotification;
import com.backend.modules.notification.entity.Notification;
import com.backend.modules.notification.entity.NotificationReception;
import com.backend.modules.notification.entity.NotificationReceptionId;
import com.backend.modules.notification.repository.NotificationReceptionRepository;
import com.backend.modules.notification.repository.NotificationRepository;
import com.backend.modules.utilisateur.entity.Utilisateur;
import com.backend.modules.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationReceptionRepository receptionRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public NotificationDto publier(NotificationDto dto) {
        Notification notification = Notification.builder()
                .code("NOT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .titre(dto.getTitre())
                .dateCreation(Instant.now())
                .build();
        notification = notificationRepository.save(notification);

        List<CanalNotification> canaux = dto.getCanaux() != null
                ? dto.getCanaux()
                : List.of(CanalNotification.IN_APP);

        if (dto.getDestinataireIds() != null) {
            for (Long uid : dto.getDestinataireIds()) {
                Utilisateur utilisateur = utilisateurRepository.findById(uid)
                        .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", uid));
                CanalNotification canal = canaux.getFirst();
                NotificationReception reception = NotificationReception.builder()
                        .notification(notification)
                        .utilisateur(utilisateur)
                        .canal(canal.name())
                        .statutEnvoi("ENVOYE")
                        .lue(false)
                        .build();
                receptionRepository.save(reception);
                if (canal == CanalNotification.IN_APP) {
                    livrerInApp(uid, notification, dto);
                }
            }
        }
        return toDto(notification, dto);
    }

    @Transactional(readOnly = true)
    public Page<NotificationReception> mesNotifications(Long utilisateurId, Pageable pageable) {
        return receptionRepository.findByUtilisateur_Id(utilisateurId, pageable);
    }

    @Transactional(readOnly = true)
    public long compterNonLues(Long utilisateurId) {
        return receptionRepository.countByUtilisateur_IdAndLueFalse(utilisateurId);
    }

    @Transactional
    public void marquerCommeLue(Long notificationId, Long utilisateurId) {
        NotificationReception reception = receptionRepository.findById(new NotificationReceptionId(notificationId, utilisateurId))
                .orElseThrow(() -> new ResourceNotFoundException("NotificationReception", "id", notificationId));
        reception.setLue(true);
        receptionRepository.save(reception);
    }

    @Transactional
    public void marquerToutesCommeLues(Long utilisateurId) {
        receptionRepository.findByUtilisateur_IdAndLueFalse(utilisateurId, Pageable.unpaged()).forEach(reception -> {
            reception.setLue(true);
            receptionRepository.save(reception);
        });
    }

    private void livrerInApp(Long utilisateurId, Notification notification, NotificationDto source) {
        try {
            messagingTemplate.convertAndSendToUser(
                    utilisateurId.toString(),
                    "/queue/notifications",
                    toDto(notification, source));
        } catch (Exception ex) {
            log.error("Erreur livraison WebSocket pour userId={} : {}", utilisateurId, ex.getMessage());
        }
    }

    private NotificationDto toDto(Notification notification, NotificationDto source) {
        return NotificationDto.builder()
                .id(notification.getId())
                .titre(notification.getTitre())
                .dateCreation(notification.getDateCreation())
                .evenement(source == null ? null : source.getEvenement())
                .corps(source == null ? null : source.getCorps())
                .payloadJson(source == null ? null : source.getPayloadJson())
                .entiteSource(source == null ? null : source.getEntiteSource())
                .idEntiteSource(source == null ? null : source.getIdEntiteSource())
                .build();
    }
}
