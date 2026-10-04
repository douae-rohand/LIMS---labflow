package com.backend.modules.demande.entity;

import com.backend.modules.client.entity.Client;
import com.backend.modules.patient.entity.Patient;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "demande", uniqueConstraints = {
        @UniqueConstraint(name = "uk_demande_numero", columnNames = "numero")
})
public class Demande {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_demande")
    private Long id;

    @Column(nullable = false, length = 50)
    private String numero;

    @Column(nullable = false, length = 255)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String objectif;

    @Column(name = "date_soumission", nullable = false)
    private Instant dateSoumission;

    @Column(nullable = false, length = 50)
    private String statut;

    @Column(name = "date_decision")
    private Instant dateDecision;

    @Column(columnDefinition = "TEXT")
    private String motif;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @Column(name = "decideur_id")
    private Long decideurId;
}
