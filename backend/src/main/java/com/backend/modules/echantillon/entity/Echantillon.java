package com.backend.modules.echantillon.entity;

import com.backend.modules.demande.entity.Demande;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "echantillon", uniqueConstraints = {
        @UniqueConstraint(name = "uk_echantillon_reference", columnNames = "reference")
})
public class Echantillon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_echantillon")
    private Long id;

    @Column(nullable = false, length = 50)
    private String reference;

    @Column(nullable = false, length = 100)
    private String nature;

    @Column(name = "date_reception", nullable = false)
    private Instant dateReception;

    private Boolean conformite;

    @Column(columnDefinition = "TEXT")
    private String motif;

    @Column(name = "etat_conservation_constate", columnDefinition = "TEXT")
    private String etatConservationConstate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "demande_id", nullable = false)
    private Demande demande;

    @Column(name = "receptionneur_id", nullable = false)
    private Long receptionneurId;
}
