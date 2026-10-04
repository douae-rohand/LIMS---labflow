package com.backend.modules.rapport.entity;

import com.backend.modules.demande.entity.Demande;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "rapport", uniqueConstraints = {
        @UniqueConstraint(name = "uk_rapport_numero", columnNames = "numero")
})
public class Rapport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rapport")
    private Long id;

    @Column(nullable = false, length = 50)
    private String numero;

    @Column(name = "type", length = 50)
    private String type;

    @Builder.Default
    private Short version = 1;

    @Column(name = "date_emission")
    private Instant dateEmission;

    @Column(length = 50)
    private String statut;

    @Column(name = "date_signature")
    private Instant dateSignature;

    @Column(name = "cle_pdf", length = 500)
    private String clePdf;

    @Builder.Default
    private Boolean diffuse = false;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "demande_id", nullable = false)
    private Demande demande;

    @Column(name = "signataire_id")
    private Long signataireId;
}
