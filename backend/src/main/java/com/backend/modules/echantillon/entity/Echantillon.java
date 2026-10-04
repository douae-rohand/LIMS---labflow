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

    @Column(length = 100)
    private String nature;

    @Column(name = "date_reception")
    private Instant dateReception;

    private Boolean conformite;

    @Column(columnDefinition = "TEXT")
    private String motif;

    @Column(name = "conditions_conservation", columnDefinition = "TEXT")
    private String conditionsConservation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "demande_id", nullable = false)
    private Demande demande;

    @Column(name = "receptionneur_id")
    private Long receptionneurId;
}
