package com.backend.common.audit;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Entité de journalisation d'audit.
 * Stockée dans le schéma tenant (table {@code journal_audit}).
 *
 * <p>Chaque action significative (création, modification, suppression,
 * validation…) génère une entrée immuable dans cette table.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "journal_audit")
public class JournalAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Code de l'action effectuée (ex. CREATION_DEMANDE). */
    @Column(nullable = false, length = 100)
    private String action;

    /** Nom de l'entité concernée (ex. Demande). */
    @Column(name = "nom_entite", length = 100)
    private String nomEntite;

    /** Identifiant de l'entité concernée. */
    @Column(name = "id_entite")
    private Long idEntite;

    /** Login ou identifiant de l'utilisateur ayant effectué l'action. */
    @Column(name = "utilisateur", length = 150)
    private String utilisateur;

    /** Description lisible de l'action. */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** Snapshot JSON de l'état avant modification (optionnel). */
    @Column(name = "ancien_etat", columnDefinition = "TEXT")
    private String ancienEtat;

    /** Snapshot JSON de l'état après modification (optionnel). */
    @Column(name = "nouvel_etat", columnDefinition = "TEXT")
    private String nouvelEtat;

    /** Adresse IP du client ayant émis la requête. */
    @Column(name = "adresse_ip", length = 45)
    private String adresseIp;

    @Builder.Default
    @Column(name = "date_action", nullable = false, updatable = false)
    private Instant dateAction = Instant.now();
}
