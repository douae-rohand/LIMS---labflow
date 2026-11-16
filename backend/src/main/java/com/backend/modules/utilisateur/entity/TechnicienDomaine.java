package com.backend.modules.utilisateur.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
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
@Table(name = "technicien_domaine")
@IdClass(TechnicienDomaineId.class)
public class TechnicienDomaine {

    @Id
    @Column(name = "technicien_id", nullable = false)
    private Long technicienId;

    @Id
    @Column(name = "domaine_id", nullable = false)
    private Long domaineId;
}
