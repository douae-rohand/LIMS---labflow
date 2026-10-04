package com.backend.modules.rapport.entity;

import com.backend.modules.essai.entity.LigneEssai;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "rapport_ligne")
@IdClass(RapportLigneId.class)
public class RapportLigne {

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rapport_id", nullable = false)
    private Rapport rapport;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ligne_essai_id", nullable = false)
    private LigneEssai ligneEssai;
}
