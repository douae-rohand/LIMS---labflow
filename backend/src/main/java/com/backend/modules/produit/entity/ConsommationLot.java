package com.backend.modules.produit.entity;

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
@Table(name = "consommation_lot")
@IdClass(ConsommationLotId.class)
public class ConsommationLot {

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ligne_essai_id", nullable = false)
    private LigneEssai ligneEssai;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lot_id", nullable = false)
    private Lot lot;

    @Column(name = "quantite_consommee", nullable = false, precision = 10, scale = 3)
    private BigDecimal quantiteConsommee;
}
