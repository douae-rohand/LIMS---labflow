package com.backend.modules.notification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Notification générée par un événement métier.
 * Stockée dans le schéma tenant ({@code notification}).
 *
 * <p>Une notification peut avoir plusieurs destinataires ({@link NotificationDestinataire}).
 * Elle est d'abord créée avec le statut {@code CREEE}, puis distribuée aux canaux
 * configurés (email, WebSocket, n8n…).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "evenement", nullable = false, length = 60)
    private EvenementMetier evenement;

    @Column(name = "titre", nullable = false, length = 300)
    private String titre;

    @Column(name = "corps", columnDefinition = "TEXT")
    private String corps;

    /** Données métier contextuelles sérialisées en JSON (ex. {demandeId: 42}). */
    @Column(name = "payload_json", columnDefinition = "TEXT")
    private String payloadJson;

    /** Entité source (ex. Demande, Essai). */
    @Column(name = "entite_source", length = 100)
    private String entiteSource;

    @Column(name = "id_entite_source")
    private Long idEntiteSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    @Builder.Default
    private StatutNotification statut = StatutNotification.CREEE;

    @Column(name = "date_creation", nullable = false, updatable = false)
    @Builder.Default
    private Instant dateCreation = Instant.now();

    @Column(name = "date_envoi")
    private Instant dateEnvoi;
}
