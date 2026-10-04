package com.backend.modules.utilisateur.entity;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UtilisateurDomaineId implements Serializable {

    private Long utilisateurId;
    private String codeDomaine;
}
