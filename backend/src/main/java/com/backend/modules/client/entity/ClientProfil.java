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

    /** Nullable : les particuliers n'ont pas de raison sociale. */
    @Column(name = "raison_sociale", length = 255)
    private String raisonSociale;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "type_client", nullable = false, length = 20)
    private TypeClient typeClient = TypeClient.PARTICULIER;

    @Column(length = 50)
    private String ice;

    @Column(columnDefinition = "TEXT")
    private String adresse;

    @Builder.Default
    @Column(name = "consentement_cndp", nullable = false)
    private Boolean consentementCndp = false;

    /** Horodatage du consentement au moment de l'inscription. */
    @Column(name = "date_consentement_cndp")
    private Instant dateConsentementCndp;

    /** Version de la politique de confidentialité acceptée (ex. "1.0"). */
    @Column(name = "version_consentement_cndp", length = 20)
    private String versionConsentementCndp;

    @Builder.Default
    @Column(name = "date_creation", nullable = false, updatable = false)
    private Instant dateCreation = Instant.now();
}
