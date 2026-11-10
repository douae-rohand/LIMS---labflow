package com.backend.modules.accueil.dto;

import com.backend.modules.client.entity.TypeClient;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;

/** Contrats de l'enregistrement des clients et des patients à l'accueil. */
public final class ClientsDtos {

    private ClientsDtos() {
    }

    public record ClientAccueilDto(
            Long utilisateurId,
            String nomComplet,
            String email,
            String telephone,
            TypeClient type,
            String raisonSociale,
            String ice,
            String adresse,
            boolean compteActive,
            Instant datePremierContact) {
    }

    public record CreerClientRequest(
            @NotNull(message = "Choisissez le type de client") TypeClient type,
            @NotBlank(message = "Le nom est obligatoire") @Size(max = 100) String nom,
            @Size(max = 100) String prenom,
            @NotBlank(message = "L'e-mail est obligatoire") @Email(message = "E-mail invalide") @Size(max = 255) String email,
            @Pattern(regexp = "^$|^\\+?[0-9 ]{8,20}$", message = "Téléphone invalide") String telephone,
            @Size(max = 255) String raisonSociale,
            @Pattern(regexp = "^$|^[0-9]{15}$", message = "L'ICE comporte 15 chiffres") String ice,
            @Size(max = 500) String adresse,
            @AssertTrue(message = "Le consentement CNDP (loi 09-08) du client est obligatoire") boolean consentementCndp) {
    }

    public record RattacherClientRequest(
            @NotBlank(message = "L'e-mail est obligatoire") @Email(message = "E-mail invalide") String email) {
    }

    public record PatientDto(
            Long id,
            String code,
            String nom,
            String prenom,
            LocalDate dateNaissance,
            String sexe,
            String cin,
            String telephone,
            String email,
            String adresse) {
    }

    public record PatientRequest(
            @NotBlank(message = "Le nom est obligatoire") @Size(max = 100) String nom,
            @NotBlank(message = "Le prénom est obligatoire") @Size(max = 100) String prenom,
            @Past(message = "La date de naissance doit être dans le passé") LocalDate dateNaissance,
            @Pattern(regexp = "^$|^[MF]$", message = "Sexe : M ou F") String sexe,
            @Pattern(regexp = "^$|^[A-Za-z]{1,2}[0-9]{3,8}$", message = "CIN invalide (ex. AB123456)") String cin,
            @Pattern(regexp = "^$|^\\+?[0-9 ]{8,20}$", message = "Téléphone invalide") String telephone,
            @Pattern(regexp = "^$|^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$", message = "E-mail invalide") @Size(max = 255) String email,
            @Size(max = 500) String adresse,
            @AssertTrue(message = "Le consentement CNDP (loi 09-08) du patient est obligatoire") boolean consentementCndp) {
    }
}
