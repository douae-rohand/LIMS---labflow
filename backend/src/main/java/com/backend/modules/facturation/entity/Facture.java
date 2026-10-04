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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "demande_id", nullable = false)
    private Demande demande;
}
