package com.backend.modules.patient.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "patient", uniqueConstraints = {
        @UniqueConstraint(name = "uk_patient_code", columnNames = "code")
})
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_patient")
    private Long id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(nullable = false, length = 100)
    private String prenom;

    @Column(name = "date_naissance")
    private LocalDate dateNaissance;

    @Column(length = 10)
    private String sexe;

    @Column(length = 20)
    private String cin;

    @Column(length = 50)
    private String telephone;

    @Column(length = 255)
    private String email;

    @Column(columnDefinition = "TEXT")
    private String adresse;
}
