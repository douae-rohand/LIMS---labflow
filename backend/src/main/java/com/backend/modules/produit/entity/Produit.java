package com.backend.modules.produit.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "produit", uniqueConstraints = {
        @UniqueConstraint(name = "uk_produit_reference", columnNames = "reference")
})
public class Produit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_produit")
    private Long id;

    @Column(nullable = false, length = 50)
    private String reference;

    @Column(nullable = false, length = 255)
    private String nom;

    @Column(nullable = false, length = 50)
    private String unite;

    @Builder.Default
    @Column(name = "seuil_minimal", precision = 10, scale = 3)
    private BigDecimal seuilMinimal = BigDecimal.ZERO;

    @Column(name = "conditions_stockage", columnDefinition = "TEXT")
    private String conditionsStockage;
}
