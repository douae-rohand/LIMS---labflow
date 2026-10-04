package com.backend.modules.notification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "notification", uniqueConstraints = {
        @UniqueConstraint(name = "uk_notification_code", columnNames = "code")
})
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notification")
    private Long id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 255)
    private String titre;

    @Column(name = "date_creation", nullable = false)
    private Instant dateCreation;
}
