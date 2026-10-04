package com.backend.modules.ia.entity;

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
@Table(name = "synthese_ia", uniqueConstraints = {
        @UniqueConstraint(name = "uk_synthese_ia_code", columnNames = "code")
})
public class SyntheseIa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_synthese")
    private Long id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenu;

    @Column(name = "date_generation", nullable = false)
    private Instant dateGeneration;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "demande_id", nullable = false)
    private Demande demande;
}
