package com.backend.modules.produit.entity;

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
@Table(name = "lot", uniqueConstraints = {
        @UniqueConstraint(name = "uk_lot_produit_numero", columnNames = {"produit_id", "numero"})
})
public class Lot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_lot")
    private Long id;

    @Column(nullable = false, length = 50)
    private String numero;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal quantite;

    @Column(nullable = false)
    private LocalDate peremption;

    @Column(nullable = false, length = 50)
    private String statut;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produit_id")
    private Produit produit;
}
