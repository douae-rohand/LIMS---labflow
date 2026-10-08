package com.backend.modules.utilisateur.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatriculeSequenceId implements Serializable {
    private String prefixe;
    private Integer annee;
}
