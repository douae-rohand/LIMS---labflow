package com.backend.modules.facturation.entity;

import com.backend.modules.essai.entity.LigneEssai;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "facture_ligne")
public class FactureLigne {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ligne")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "facture_id", nullable = false)
    private Facture facture;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ligne_essai_id")
    private LigneEssai ligneEssai;

    @Column(nullable = false, length = 255)
    private String designation;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montant;
}
