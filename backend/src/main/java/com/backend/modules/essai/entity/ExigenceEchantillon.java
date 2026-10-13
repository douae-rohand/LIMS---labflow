package com.backend.modules.essai.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "exigence_echantillon", uniqueConstraints = {
        @UniqueConstraint(name = "uk_exigence_essai_nature", columnNames = {"essai_id", "nature"})
})
public class ExigenceEchantillon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_exigence")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "essai_id", nullable = false)
    private Essai essai;

    @Column(nullable = false, length = 100)
    private String nature;

    @Column(name = "etat_conservation", nullable = false, length = 255)
    private String etatConservation;

    @Column(name = "quantite_minimale", precision = 10, scale = 2)
    private BigDecimal quantiteMinimale;

    @Column(length = 20)
    private String unite;
}
