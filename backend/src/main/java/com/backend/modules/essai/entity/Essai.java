package com.backend.modules.essai.entity;

import com.backend.modules.domaine.entity.Domaine;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "essai", uniqueConstraints = {
        @UniqueConstraint(name = "uk_essai_code", columnNames = "code")
})
public class Essai {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_essai")
    private Long id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 255)
    private String designation;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 255)
    private String methode;

    @Builder.Default
    @Column(precision = 12, scale = 2)
    private BigDecimal tarif = BigDecimal.ZERO;

    @Column(name = "duree_estimee", nullable = false)
    private Integer dureeEstimee;

    @Column(length = 50)
    private String unite;

    @Column(name = "limite_min", precision = 12, scale = 4)
    private BigDecimal limiteMin;

    @Column(name = "limite_max", precision = 12, scale = 4)
    private BigDecimal limiteMax;

    @Column(name = "seuil_critique_min", precision = 12, scale = 4)
    private BigDecimal seuilCritiqueMin;

    @Column(name = "seuil_critique_max", precision = 12, scale = 4)
    private BigDecimal seuilCritiqueMax;

    @Builder.Default
    private Boolean actif = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "domaine_id")
    private Domaine domaine;
}
