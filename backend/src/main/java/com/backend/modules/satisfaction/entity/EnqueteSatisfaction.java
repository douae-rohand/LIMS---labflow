package com.backend.modules.satisfaction.entity;

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
@Table(name = "enquete_satisfaction", uniqueConstraints = {
        @UniqueConstraint(name = "uk_enquete_satisfaction_code", columnNames = "code")
})
public class EnqueteSatisfaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_enquete")
    private Long id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(name = "date_envoi")
    private Instant dateEnvoi;

    @Column(name = "date_reponse")
    private Instant dateReponse;

    @Column(name = "note_globale")
    private Integer noteGlobale;

    @Column(name = "note_delai")
    private Integer noteDelai;

    @Column(name = "note_clarte")
    private Integer noteClarte;

    @Column(name = "note_relation")
    private Integer noteRelation;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    @Builder.Default
    @Column(name = "relance_envoyee")
    private Boolean relanceEnvoyee = false;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "demande_id", nullable = false)
    private Demande demande;
}
