package com.backend.modules.stock.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/**
 * Mouvement de stock (entrée, sortie, ajustement).
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class MouvementStock {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "article_id", nullable = false)
    private Long articleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_mouvement", nullable = false, length = 20)
    private TypeMouvement typeMouvement;

    @Column(name = "quantite", nullable = false)
    private Double quantite;

    @Column(name = "motif", length = 300)
    private String motif;

    @Column(name = "operateur_id")
    private Long operateurId;

    @Column(name = "date_mouvement", nullable = false, updatable = false)
    @Builder.Default
    private Instant dateMouvement = Instant.now();
}
