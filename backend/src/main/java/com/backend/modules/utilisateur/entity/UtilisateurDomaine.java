package com.backend.modules.utilisateur.entity;

import com.backend.common.config.SchemaConstants;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "utilisateur_domaine", schema = SchemaConstants.CENTRAL_SCHEMA)
@IdClass(UtilisateurDomaineId.class)
public class UtilisateurDomaine {

    @Id
    @Column(name = "utilisateur_id", nullable = false)
    private Long utilisateurId;

    @Id
    @Column(name = "code_domaine", nullable = false, length = 50)
    private String codeDomaine;
}
