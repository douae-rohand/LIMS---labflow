package com.backend.modules.utilisateur.entity;

import com.backend.common.config.SchemaConstants;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@IdClass(MatriculeSequenceId.class)
@Table(name = "matricule_sequence", schema = SchemaConstants.CENTRAL_SCHEMA)
public class MatriculeSequence {

    @Id
    @Column(length = 10, nullable = false)
    private String prefixe;

    @Id
    @Column(nullable = false)
    private Integer annee;

    @Column(name = "dernier_numero", nullable = false)
    private Integer dernierNumero;
}
