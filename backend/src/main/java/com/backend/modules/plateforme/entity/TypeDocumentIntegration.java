package com.backend.modules.plateforme.entity;

import com.backend.common.config.SchemaConstants;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Document exigé pour une demande d'intégration. La liste est paramétrée
 * par le Super Administrateur ; un type désactivé n'est plus demandé mais
 * reste lisible sur les dossiers qui le contiennent déjà.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "type_document_integration", schema = SchemaConstants.CENTRAL_SCHEMA)
public class TypeDocumentIntegration {

    @Id
    @Column(length = 80)
    private String code;

    @Column(nullable = false, length = 255)
    private String libelle;

    @Column(length = 255)
    private String raison;

    @Builder.Default
    @Column(nullable = false)
    private boolean obligatoire = true;

    @Builder.Default
    @Column(nullable = false)
    private boolean actif = true;

    @Builder.Default
    @Column(nullable = false)
    private int ordre = 0;

    @Builder.Default
    @Column(name = "date_creation", nullable = false, updatable = false)
    private Instant dateCreation = Instant.now();
}
