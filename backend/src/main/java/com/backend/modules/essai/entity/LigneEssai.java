package com.backend.modules.essai.entity;

import com.backend.modules.demande.entity.Demande;
import com.backend.modules.echantillon.entity.Echantillon;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ligne_essai", uniqueConstraints = {
        @UniqueConstraint(name = "uk_ligne_essai_code", columnNames = "code")
})
public class LigneEssai {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ligne_essai")
    private Long id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(length = 50)
    private String statut;

    @Column(name = "date_attribution")
    private Instant dateAttribution;

    @Column(name = "date_debut")
    private Instant dateDebut;

    @Column(name = "date_fin")
    private Instant dateFin;

    @Column(name = "duree_minutes")
    private Integer dureeMinutes;

    @Column(length = 255)
    private String valeur;

    @Column(name = "date_saisie")
    private Instant dateSaisie;

    private Boolean conformite;

    @Column(name = "score_anomalie", precision = 5, scale = 2)
    private BigDecimal scoreAnomalie;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montant;

    @Column(columnDefinition = "TEXT")
    private String motif;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "demande_id", nullable = false)
    private Demande demande;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "essai_id", nullable = false)
    private Essai essai;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "echantillon_id")
    private Echantillon echantillon;

    @Column(name = "technicien_id")
    private Long technicienId;
}
