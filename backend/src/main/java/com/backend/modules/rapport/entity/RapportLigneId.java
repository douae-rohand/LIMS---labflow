package com.backend.modules.rapport.entity;

import com.backend.modules.essai.entity.LigneEssai;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class RapportLigneId implements Serializable {

    private Long rapport;
    private Long ligneEssai;
}
