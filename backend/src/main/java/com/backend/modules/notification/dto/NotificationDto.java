package com.backend.modules.notification.dto;

import com.backend.modules.notification.entity.CanalNotification;
import com.backend.modules.notification.entity.EvenementMetier;
import com.backend.modules.notification.entity.StatutNotification;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {
    private Long id;
    private EvenementMetier evenement;
    private String titre;
    private String corps;
    private String payloadJson;
    private String entiteSource;
    private Long idEntiteSource;
    private StatutNotification statut;
    private Instant dateCreation;
    private Instant dateEnvoi;

    /** Destinataires (inclus uniquement à la création). */
    private List<Long> destinataireIds;
    private List<CanalNotification> canaux;
}
