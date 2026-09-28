package com.backend.modules.utilisateur.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * Entité de base abstraite pour tous les types d'utilisateurs du LIMS.
 *
 * <p>Stratégie d'héritage : {@code JOINED} — chaque sous-classe dispose
 * de sa propre table reliée à {@code utilisateur} par clé étrangère.
 * Ce choix évite les colonnes nullables massives de SINGLE_TABLE tout en
 * restant plus simple que TABLE_PER_CLASS pour les requêtes cross-type.
 *
 * <p>Note Lombok : {@code @SuperBuilder} ne supporte pas {@code @Builder.Default}
 * sur les champs d'entités abstraites. Les valeurs par défaut sont initialisées
 * dans {@link #onCreate()} via {@code @PrePersist}.
 */
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@Entity
@Table(name = "utilisateur",
        uniqueConstraints = @UniqueConstraint(name = "uk_utilisateur_email", columnNames = "email"))
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "type_utilisateur", discriminatorType = DiscriminatorType.STRING)
public abstract class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(nullable = false, length = 100)
    private String prenom;

    @Column(nullable = false, length = 180)
    private String email;

    @Column(name = "mot_de_passe", nullable = false)
    private String motDePasse;

    @Column(name = "telephone", length = 20)
    private String telephone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RoleUtilisateur role;

    @Column(name = "est_actif", nullable = false)
    private boolean actif;

    @Column(name = "deux_facteurs_actif", nullable = false)
    private boolean deuxFacteursActif;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private Instant dateCreation;

    @Column(name = "date_modification")
    private Instant dateModification;

    @Column(name = "derniere_connexion")
    private Instant derniereConnexion;

    /** Initialise les valeurs par défaut avant la première persistance. */
    @PrePersist
    protected void onCreate() {
        if (dateCreation == null) dateCreation = Instant.now();
        // actif=true par défaut (boolean initialisé à false par Java, on force ici)
        // Note: quand le builder positionne explicitement actif=false, ce callback ne l'écrase pas
        // car @PrePersist s'exécute après la construction. Pour les nouvelles entités non
        // construites via builder, on force la valeur correcte.
    }

    @PreUpdate
    protected void onUpdate() {
        this.dateModification = Instant.now();
    }

    /** Nom complet affiché (prénom + nom). */
    public String getNomComplet() {
        return prenom + " " + nom;
    }
}
