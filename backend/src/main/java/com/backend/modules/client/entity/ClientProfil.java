package com.backend.modules.client.entity;

import com.backend.common.config.SchemaConstants;
import com.backend.modules.utilisateur.entity.Utilisateur;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "client_profil", schema = SchemaConstants.CENTRAL_SCHEMA, uniqueConstraints = {
        @UniqueConstraint(name = "uk_client_profil_utilisateur", columnNames = "utilisateur_id")
})
public class ClientProfil {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_client_profil")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false, unique = true)
    private Utilisateur utilisateur;

    @Column(name = "raison_sociale", nullable = false, length = 255)
    private String raisonSociale;

    @Column(length = 50)
    private String ice;

    @Column(columnDefinition = "TEXT")
    private String adresse;

    @Builder.Default
    @Column(name = "consentement_cndp", nullable = false)
    private Boolean consentementCndp = false;

    @Builder.Default
    @Column(name = "date_creation", nullable = false, updatable = false)
    private Instant dateCreation = Instant.now();
}
