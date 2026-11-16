package com.backend.modules.utilisateur.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class TechnicienDomaineId implements Serializable {

    private Long technicienId;
    private Long domaineId;
}
