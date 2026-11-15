package com.backend.modules.facturation.entity;

import com.backend.modules.demande.entity.Demande;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "facture", uniqueConstraints = {
        @UniqueConstraint(name = "uk_facture_numero", columnNames = "numero")
})
public class Facture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_facture")
    private Long id;

    @Column(nullable = false, length = 50)
    private String numero;

    @Column(name = "type", length = 50)
    private String type;

    @Column(name = "date_facture")
    private LocalDate dateFacture;

    @Column(name = "date_echeance")
    private LocalDate dateEcheance;

    @Column(name = "montant_ht", nullable = false, precision = 12, scale = 2)
    private BigDecimal montantHt;

    @Column(length = 50)
    private String statut;

    /** Taux de commission de la plateforme (%) figé à l'émission ; {@code montantHt} porte le montant facturé. */
    @Column(name = "taux_commission", precision = 5, scale = 2)
    private BigDecimal tauxCommission;

    /** Une facture n'est jamais supprimée : elle est annulée par un avoir AV-AAAAMM-NNN. */
    @Column(name = "numero_avoir", length = 50)
    private String numeroAvoir;

    @Column(name = "date_annulation")
    private java.time.Instant dateAnnulation;

    @Column(name = "motif_annulation", length = 500)
    private String motifAnnulation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "demande_id", nullable = false)
    private Demande demande;
}
