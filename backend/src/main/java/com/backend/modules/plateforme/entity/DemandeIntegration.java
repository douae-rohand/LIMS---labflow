package com.backend.modules.plateforme.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "demande_integration", uniqueConstraints = {
        @UniqueConstraint(name = "uk_demande_integration_numero", columnNames = "numero")
})
public class DemandeIntegration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_demande_integ")
    private Long id;

    @Column(nullable = false, length = 50)
    private String numero;

    @Column(name = "date_demande", nullable = false)
    private Instant dateDemande;

    @Column(nullable = false, length = 50)
    private String statut;

    @Column(name = "motif_refus", columnDefinition = "TEXT")
    private String motifRefus;

    @Column(name = "raison_sociale", nullable = false, length = 255)
    private String raisonSociale;

    @Column(length = 50)
    private String ice;

    @Column(columnDefinition = "TEXT")
    private String adresse;

    @Column(name = "contact_nom", nullable = false, length = 255)
    private String contactNom;

    @Column(name = "contact_email", nullable = false, length = 255)
    private String contactEmail;

    @Column(name = "contact_telephone", length = 50)
    private String contactTelephone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "laboratoire_id")
    private Laboratoire laboratoire;
}
