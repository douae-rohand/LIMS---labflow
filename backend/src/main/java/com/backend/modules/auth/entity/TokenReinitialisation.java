package com.backend.modules.auth.entity;

import com.backend.common.config.SchemaConstants;
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
@Table(name = "token_reinitialisation", schema = SchemaConstants.CENTRAL_SCHEMA, uniqueConstraints = {
        @UniqueConstraint(name = "uk_token_reinitialisation_token_hash", columnNames = "token_hash")
})
public class TokenReinitialisation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_token_reset")
    private Long id;

    @Column(name = "token_hash", nullable = false, length = 255)
    private String tokenHash;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "date_expiration", nullable = false)
    private Instant dateExpiration;

    @Builder.Default
    @Column(nullable = false)
    private boolean utilise = false;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;
}
