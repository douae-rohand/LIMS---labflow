package com.backend.modules.utilisateur.entity;

import com.backend.common.config.SchemaConstants;
import com.backend.modules.plateforme.entity.Laboratoire;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@Entity
@Table(name = "utilisateur", schema = SchemaConstants.CENTRAL_SCHEMA, uniqueConstraints = {
        @UniqueConstraint(name = "uk_utilisateur_matricule", columnNames = "matricule"),
        @UniqueConstraint(name = "uk_utilisateur_email", columnNames = "email")
})
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_utilisateur")
    private Long id;

    @Column(length = 50)
    private String matricule;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(length = 100)
    private String prenom;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(length = 50)
    private String telephone;

    @Column(length = 20)
    private String cin;

    @Column(length = 100)
    private String fonction;

    @Column(name = "mot_de_passe_hash", nullable = false, length = 255)
    private String motDePasseHash;

    @Builder.Default
    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword = false;

    @Builder.Default
    @Column(name = "double_authentification", nullable = false)
    private boolean doubleAuthentification = false;

    @Column(name = "secret_2fa", length = 255)
    private String secret2fa;

    @Column(name = "derniere_connexion")
    private Instant derniereConnexion;

    @Builder.Default
    @Column(nullable = false)
    private boolean actif = true;

    @Builder.Default
    @Column(name = "compte_confirme", nullable = false)
    private boolean compteConfirme = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "laboratoire_id")
    private Laboratoire laboratoire;

    public String getNomComplet() {
        if (prenom == null || prenom.isBlank()) {
            return nom;
        }
        return prenom + " " + nom;
    }
}
