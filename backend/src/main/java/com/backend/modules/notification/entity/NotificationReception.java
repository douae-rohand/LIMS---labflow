package com.backend.modules.notification.entity;

import com.backend.modules.utilisateur.entity.Utilisateur;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "notification_reception")
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

    @Column(length = 30)
    private String canal;

    @Column(name = "statut_envoi", length = 30)
    private String statutEnvoi;

    @Builder.Default
    @Column(nullable = false)
    private boolean lue = false;
}
