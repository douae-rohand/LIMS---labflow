package com.backend.modules.validation.entity;

import com.backend.modules.essai.entity.LigneEssai;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "validation", uniqueConstraints = {
        @UniqueConstraint(name = "uk_validation_code", columnNames = "code")
})
public class Validation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_validation")
    private Long id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false)
    private Integer niveau;

    @Column(length = 50)
    private String decision;

    @Column(columnDefinition = "TEXT")
    private String motif;

    @Column(name = "date_validation", nullable = false)
    private Instant dateValidation;

    @Column(length = 255)
    private String signature;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ligne_essai_id", nullable = false)
    private LigneEssai ligneEssai;

    @Column(name = "validateur_id", nullable = false)
    private Long validateurId;
}
