package com.backend.modules.client.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "client", uniqueConstraints = {
        @UniqueConstraint(name = "uk_client_code", columnNames = "code")
})
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_client")
    private Long id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(name = "raison_sociale", nullable = false, length = 255)
    private String raisonSociale;

    @Column(length = 50)
    private String ice;

    @Column(columnDefinition = "TEXT")
    private String adresse;

    @Builder.Default
    @Column(name = "consentement_cndp")
    private Boolean consentementCndp = false;

    @Column(name = "utilisateur_id")
    private Long utilisateurId;
}
