package com.backend.modules.facturation.entity;

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
@Table(name = "paiement", uniqueConstraints = {
        @UniqueConstraint(name = "uk_paiement_code", columnNames = "code")
})
public class Paiement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_paiement")
    private Long id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(name = "date_paiement", nullable = false)
    private Instant datePaiement;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montant;

    @Column(length = 50)
    private String mode;

    @Column(length = 255)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "facture_id", nullable = false)
    private Facture facture;

    @Column(name = "encaisseur_id")
    private Long encaisseurId;
}
