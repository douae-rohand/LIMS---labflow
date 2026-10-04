package com.backend.modules.demande.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "piece_jointe", uniqueConstraints = {
        @UniqueConstraint(name = "uk_piece_jointe_code", columnNames = "code")
})
public class PieceJointe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_piece")
    private Long id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(name = "nom_fichier", length = 255)
    private String nomFichier;

    @Column(name = "type_mime", length = 100)
    private String typeMime;

    private Long taille;

    @Column(name = "cle_objet", length = 500)
    private String cleObjet;

    @Column(name = "date_ajout")
    private Instant dateAjout;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "demande_id", nullable = false)
    private Demande demande;
}
