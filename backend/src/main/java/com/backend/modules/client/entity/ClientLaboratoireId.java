package com.backend.modules.client.entity;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ClientLaboratoireId implements Serializable {

    private Long utilisateur;
    private Long laboratoire;
}
