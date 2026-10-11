package com.backend.modules.notification.entity;

import com.backend.common.config.SchemaConstants;
import com.backend.modules.utilisateur.entity.Utilisateur;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "notification_reception", schema = SchemaConstants.CENTRAL_SCHEMA)
@IdClass(NotificationReceptionId.class)
public class NotificationReception {

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private CanalNotification canal;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_envoi", length = 30)
    private StatutNotification statutEnvoi;

    @Builder.Default
    @Column(nullable = false)
    private boolean lue = false;
}
