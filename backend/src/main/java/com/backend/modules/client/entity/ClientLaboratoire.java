package com.backend.modules.client.entity;

import com.backend.common.config.SchemaConstants;
import com.backend.modules.plateforme.entity.Laboratoire;
import com.backend.modules.utilisateur.entity.Utilisateur;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "client_laboratoire", schema = SchemaConstants.CENTRAL_SCHEMA)
@IdClass(ClientLaboratoireId.class)
public class ClientLaboratoire {

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "laboratoire_id", nullable = false)
    private Laboratoire laboratoire;

    @Column(name = "client_local_id", nullable = false)
    private Long clientLocalId;

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String statut = "ACTIF";

    @Builder.Default
    @Column(name = "date_premier_contact", nullable = false)
    private Instant datePremierContact = Instant.now();
}
