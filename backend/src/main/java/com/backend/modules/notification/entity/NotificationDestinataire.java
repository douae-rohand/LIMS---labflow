package com.backend.modules.notification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Association entre une {@link Notification} et un destinataire.
 * Trace le statut de livraison par canal.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDestinataire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    /** Identifiant de l'utilisateur destinataire (clé logique). */
    @Column(name = "utilisateur_id", nullable = false)
    private Long utilisateurId;

    /** Email utilisé pour l'envoi (snapshot au moment de la création). */
    @Column(name = "email_destinataire", length = 180)
    private String emailDestinataire;

    @Enumerated(EnumType.STRING)
    @Column(name = "canal", nullable = false, length = 20)
    private CanalNotification canal;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_livraison", nullable = false, length = 20)
    @Builder.Default
    private StatutLivraison statutLivraison = StatutLivraison.EN_ATTENTE;

    /** Lu par l'utilisateur (uniquement pertinent pour IN_APP). */
    @Column(name = "est_lu")
    @Builder.Default
    private boolean lu = false;

    @Column(name = "date_lecture")
    private Instant dateLecture;

    @Column(name = "date_livraison")
    private Instant dateLivraison;

    @Column(name = "erreur", columnDefinition = "TEXT")
    private String erreur;

    @Column(name = "date_creation", nullable = false, updatable = false)
    @Builder.Default
    private Instant dateCreation = Instant.now();

    public enum StatutLivraison {
        EN_ATTENTE, LIVRE, ECHEC
    }
}
