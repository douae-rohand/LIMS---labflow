package com.backend.common.audit;

import com.backend.modules.utilisateur.entity.Utilisateur;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "journal_audit", uniqueConstraints = {
        @UniqueConstraint(name = "uk_journal_audit_code", columnNames = "code")
})
public class JournalAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_audit")
    private Long id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(name = "date_action", nullable = false)
    private Instant dateAction;

    @Column(length = 100)
    private String action;

    @Column(length = 100)
    private String objet;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "valeur_avant", columnDefinition = "json")
    private String valeurAvant;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "valeur_apres", columnDefinition = "json")
    private String valeurApres;

    @Column(columnDefinition = "TEXT")
    private String motif;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;
}
