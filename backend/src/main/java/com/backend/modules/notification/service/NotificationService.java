package com.backend.modules.notification.service;

import com.backend.common.exception.BusinessRuleException;
import com.backend.common.exception.ResourceNotFoundException;
import com.backend.common.security.SecurityUtils;
import com.backend.modules.auth.security.UtilisateurPrincipal;
import com.backend.modules.notification.dto.NotificationDto;
import com.backend.modules.notification.entity.CanalNotification;
import com.backend.modules.notification.entity.Notification;
import com.backend.modules.notification.entity.NotificationReception;
import com.backend.modules.notification.entity.StatutEnvoi;
import com.backend.modules.notification.entity.NotificationReceptionId;
import com.backend.modules.notification.repository.NotificationReceptionRepository;
import com.backend.modules.notification.repository.NotificationRepository;
import com.backend.modules.utilisateur.entity.Utilisateur;
import com.backend.modules.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.backend.modules.utilisateur.entity.RoleUtilisateur;
import org.springframework.data.domain.PageRequest;
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
                .corps(dto.getCorps())
                .lien(dto.getLien())
                .type(dto.getType())
                .dateCreation(Instant.now())
                .build();
        notification = notificationRepository.save(notification);

        List<CanalNotification> canaux = dto.getCanaux() != null
                ? dto.getCanaux()
                : List.of(CanalNotification.IN_APP);

        if (dto.getDestinataireIds() != null) {
            UtilisateurPrincipal emetteur = SecurityUtils.principalCourant();
            for (Long uid : dto.getDestinataireIds()) {
                Utilisateur utilisateur = utilisateurRepository.findById(uid).orElse(null);
                if (utilisateur == null) {
                    if (emetteur.isSuperAdministrateur()) {
                        throw new ResourceNotFoundException("Utilisateur", "id", uid);
                    }
                    throw new BusinessRuleException("DESTINATAIRE_HORS_LABORATOIRE",
                            "Les destinataires doivent appartenir à votre laboratoire.");
                }
                if (!emetteur.isSuperAdministrateur()) {
                    Long laboEmetteur = emetteur.getLaboratoireId();
                    Long laboDestinataire = utilisateur.getLaboratoire() == null
                            ? null : utilisateur.getLaboratoire().getId();
                    if (laboEmetteur == null || !laboEmetteur.equals(laboDestinataire)) {
                        throw new BusinessRuleException("DESTINATAIRE_HORS_LABORATOIRE",
                                "Les destinataires doivent appartenir à votre laboratoire.");
                    }
                }
                CanalNotification canal = canaux.getFirst();
                NotificationReception reception = NotificationReception.builder()
                        .notification(notification)
                        .utilisateur(utilisateur)
                        .canal(canal)
                        .statutEnvoi(StatutEnvoi.ENVOYE)
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

    /**
     * Notifie tous les comptes actifs d'un rôle (ex. les Super Administrateurs
     * à chaque nouvelle demande d'intégration). Ne fait jamais échouer l'appelant.
     */
    /**
     * Notification in-app à des utilisateurs précis (client d'une demande, techniciens affectés…),
     * choisis par le code métier et non par l'utilisateur. Ne fait jamais échouer l'appelant.
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void notifierUtilisateurs(java.util.Collection<Long> utilisateurIds, String type, String titre,
                                     String corps, String lien) {
        if (utilisateurIds == null || utilisateurIds.isEmpty()) {
            return;
        }
        try {
            List<Utilisateur> destinataires = utilisateurRepository.findAllById(new java.util.HashSet<>(utilisateurIds));
            if (destinataires.isEmpty()) {
                return;
            }
            Notification notification = notificationRepository.save(Notification.builder()
                    .code("NOT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .titre(titre)
                    .corps(corps)
                    .lien(lien)
                    .type(type)
                    .dateCreation(Instant.now())
                    .build());
            for (Utilisateur utilisateur : destinataires) {
                receptionRepository.save(NotificationReception.builder()
                        .notification(notification)
                        .utilisateur(utilisateur)
                        .canal(CanalNotification.IN_APP)
                        .statutEnvoi(StatutEnvoi.ENVOYE)
                        .lue(false)
                        .build());
                livrerInApp(utilisateur.getId(), notification, NotificationDto.builder().build());
            }
        } catch (RuntimeException ex) {
            log.error("Notification « {} » non enregistrée : {}", titre, ex.getMessage());
        }
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void notifierRole(RoleUtilisateur role, String type, String titre, String corps, String lien) {
        try {
            List<Utilisateur> destinataires = utilisateurRepository.findActifsParRole(role.name());
            if (destinataires.isEmpty()) {
                return;
            }
            Notification notification = notificationRepository.save(Notification.builder()
                    .code("NOT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .titre(titre)
                    .corps(corps)
                    .lien(lien)
                    .type(type)
                    .dateCreation(Instant.now())
                    .build());
            for (Utilisateur utilisateur : destinataires) {
                receptionRepository.save(NotificationReception.builder()
                        .notification(notification)
                        .utilisateur(utilisateur)
                        .canal(CanalNotification.IN_APP)
                        .statutEnvoi(StatutEnvoi.ENVOYE)
                        .lue(false)
                        .build());
            }
        } catch (RuntimeException ex) {
            log.error("Notification « {} » non enregistrée : {}", titre, ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> mesNotifications(Long utilisateurId, int limite) {
        int taille = Math.min(Math.max(limite, 1), 50);
        return receptionRepository.listerRecentes(utilisateurId, PageRequest.of(0, taille)).stream()
                .map(reception -> {
                    Notification notification = reception.getNotification();
                    return NotificationDto.builder()
                            .id(notification.getId())
                            .titre(notification.getTitre())
                            .corps(notification.getCorps())
                            .lien(notification.getLien())
                            .type(notification.getType())
                            .dateCreation(notification.getDateCreation())
                            .statut(reception.getStatutEnvoi())
                            .lue(reception.isLue())
                            .dateLecture(reception.getDateLecture())
                            .build();
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public long compterNonLues(Long utilisateurId) {
        return receptionRepository.countByUtilisateur_IdAndLueFalse(utilisateurId);
    }

    @Transactional
    public void marquerCommeLue(Long notificationId, Long utilisateurId) {
        NotificationReception reception = receptionRepository.findById(new NotificationReceptionId(notificationId, utilisateurId))
                .orElseThrow(() -> new ResourceNotFoundException("NotificationReception", "id", notificationId));
        if (!reception.isLue()) {
            reception.setLue(true);
            reception.setDateLecture(Instant.now());
            receptionRepository.save(reception);
        }
    }

    @Transactional
    public void marquerToutesCommeLues(Long utilisateurId) {
        receptionRepository.marquerToutesLues(utilisateurId, Instant.now());
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
                .lien(notification.getLien())
                .type(notification.getType())
                .statut(StatutEnvoi.ENVOYE)
                .build();
    }
}
