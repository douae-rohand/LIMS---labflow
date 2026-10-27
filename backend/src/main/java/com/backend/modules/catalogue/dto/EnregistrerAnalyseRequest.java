package com.backend.modules.catalogue.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Création ou modification d'une analyse du catalogue du laboratoire courant.
 * Le laboratoire n'est pas un paramètre : il est celui du compte connecté.
 */
@Data
public class EnregistrerAnalyseRequest {

    @NotBlank(message = "Le code est obligatoire")
    @Size(max = 50, message = "Le code ne peut pas dépasser 50 caractères")
    @Pattern(
            regexp = "^[A-Za-z0-9][A-Za-z0-9_\\-]{0,49}$",
            message = "Le code ne peut contenir que des lettres, chiffres, tirets et underscores")
    private String code;

    @NotBlank(message = "La désignation est obligatoire")
    @Size(max = 255, message = "La désignation ne peut pas dépasser 255 caractères")
    private String designation;

    @Size(max = 4000, message = "La description ne peut pas dépasser 4000 caractères")
    private String description;

    @Size(max = 255, message = "La méthode ne peut pas dépasser 255 caractères")
    private String normeReference;

    /** Durée estimée en minutes, comme sur la fiche publique. */
    @Min(value = 0, message = "La durée estimée ne peut pas être négative")
    @Max(value = 43200, message = "La durée estimée ne peut pas dépasser 30 jours")
    private Integer dureeEstimee;

    /**
     * Alias historique de {@link #dureeEstimee}. La valeur est stockée en minutes,
     * pas en jours.
     */
    @Min(value = 0, message = "La durée estimée ne peut pas être négative")
    @Max(value = 43200, message = "La durée estimée ne peut pas dépasser 30 jours")
    private Integer delaiJours;

    @DecimalMin(value = "0.00", message = "Le prix ne peut pas être négatif")
    @Digits(integer = 10, fraction = 2, message = "Le prix est invalide")
    private BigDecimal prixUnitaire;

    @Size(max = 50, message = "L'unité ne peut pas dépasser 50 caractères")
    private String uniteMesure;

    @Size(max = 50, message = "Le code de domaine ne peut pas dépasser 50 caractères")
    private String domaineCode;

    @AssertTrue(message = "La durée estimée est obligatoire")
    public boolean isDureeRenseignee() {
        return dureeEstimee != null || delaiJours != null;
    }

    public int dureeEffective() {
        return dureeEstimee != null ? dureeEstimee : delaiJours;
    }
}
