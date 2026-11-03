package com.backend.modules.plateforme.entity;

import com.backend.common.config.SchemaConstants;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "historique_statut_laboratoire", schema = SchemaConstants.CENTRAL_SCHEMA)
public class HistoriqueStatutLaboratoire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "laboratoire_id", nullable = false)
    private Laboratoire laboratoire;

    @Column(name = "ancien_statut", length = 20)
    private String ancienStatut;

    @Column(name = "nouveau_statut", nullable = false, length = 20)
    private String nouveauStatut;

    @Column(name = "motif", columnDefinition = "TEXT")
    private String motif;

    @Column(name = "acteur_email", nullable = false, length = 255)
    private String acteurEmail;

    @Column(name = "date_changement", nullable = false)
    private Instant dateChangement;
}
