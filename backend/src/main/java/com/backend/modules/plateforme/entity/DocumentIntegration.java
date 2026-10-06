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
@Table(name = "document_integration", schema = SchemaConstants.CENTRAL_SCHEMA, uniqueConstraints = {
        @UniqueConstraint(name = "uk_document_integration_demande_type",
                columnNames = {"demande_id", "type_document"})
})
public class DocumentIntegration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_document")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "demande_id", nullable = false)
    private DemandeIntegration demande;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_document", nullable = false, length = 80)
    private TypeDocumentIntegration typeDocument;

    @Column(name = "nom_fichier", nullable = false, length = 255)
    private String nomFichier;

    @Column(name = "type_mime", nullable = false, length = 120)
    private String typeMime;

    @Column(nullable = false)
    private Long taille;

    @Column(name = "chemin_stockage", nullable = false, length = 500)
    private String cheminStockage;

    @Builder.Default
    @Column(name = "date_ajout", nullable = false, updatable = false)
    private Instant dateAjout = Instant.now();
}
