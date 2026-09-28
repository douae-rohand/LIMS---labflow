package com.backend.modules.stock.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Article géré en stock (réactif, consommable, équipement) (M09).
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "article_stock")
public class ArticleStock {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String reference;

    @Column(nullable = false, length = 300)
    private String designation;

    @Enumerated(EnumType.STRING)
    @Column(name = "categorie", length = 50)
    private CategorieStock categorie;

    @Builder.Default
    @Column(name = "quantite_disponible")
    private Double quantiteDisponible = 0.0;

    @Column(name = "quantite_minimum")
    private Double quantiteMinimum;

    @Column(name = "unite", length = 20)
    private String unite;

    @Column(name = "prix_unitaire", precision = 12, scale = 2)
    private BigDecimal prixUnitaire;

    @Column(name = "fournisseur", length = 200)
    private String fournisseur;

    @Column(name = "est_actif", nullable = false)
    @Builder.Default
    private boolean actif = true;

    @Column(name = "date_creation", nullable = false, updatable = false)
    @Builder.Default
    private Instant dateCreation = Instant.now();
}
