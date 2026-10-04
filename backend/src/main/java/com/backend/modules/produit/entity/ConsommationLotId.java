package com.backend.modules.produit.entity;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ConsommationLotId implements Serializable {

    private Long ligneEssai;
    private Long lot;
}
