package com.backend.modules.plateforme.entity;

import com.backend.common.config.SchemaConstants;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "demande_integration", schema = SchemaConstants.CENTRAL_SCHEMA, uniqueConstraints = {
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

    @Column(name = "date_traitement")
    private Instant dateTraitement;

    @Column(nullable = false, length = 50)
    private String statut;

    @Column(name = "motif_refus", columnDefinition = "TEXT")
    private String motifRefus;

    @Column(name = "nom_laboratoire", length = 255)
    private String nomLaboratoire;

    @Column(name = "raison_sociale", nullable = false, length = 255)
    private String raisonSociale;

    @Column(name = "type_laboratoire", length = 500)
    private String typeLaboratoire;

    @Column(length = 50)
    private String ice;

    @Column(name = "telephone_laboratoire", length = 50)
    private String telephoneLaboratoire;

    @Column(name = "email_laboratoire", length = 255)
    private String emailLaboratoire;

    @Column(name = "site_web", length = 255)
    private String siteWeb;

    @Column(columnDefinition = "TEXT")
    private String adresse;

    @Column(length = 100)
    private String ville;

    @Column(length = 100)
    private String region;

    @Column(length = 100)
    private String pays;

    @Column(name = "code_postal", length = 20)
    private String codePostal;

    private Double latitude;

    private Double longitude;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "contact_nom", nullable = false, length = 255)
    private String contactNom;

    @Column(name = "contact_prenom", length = 100)
    private String contactPrenom;

    @Column(name = "contact_email", nullable = false, length = 255)
    private String contactEmail;

    @Column(name = "contact_telephone", length = 50)
    private String contactTelephone;

    @Column(name = "contact_fonction", length = 100)
    private String contactFonction;

    @Column(name = "contact_cin", length = 20)
    private String contactCin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "laboratoire_id")
    private Laboratoire laboratoire;

    @Builder.Default
    @OneToMany(mappedBy = "demande", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DocumentIntegration> documents = new ArrayList<>();
}
