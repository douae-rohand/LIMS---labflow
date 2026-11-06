package com.backend.modules.catalogue.dto;

import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreerEssaiRequest {
    @NotBlank(message = "Le code est obligatoire")
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "Le code doit être en majuscules, alphanumérique avec tirets et underscores")
    private String code;

    @NotBlank(message = "La désignation est obligatoire")
    @Size(max = 255, message = "La désignation ne peut pas dépasser 255 caractères")
    private String designation;

    private String description;

    private String methode;

    @NotNull(message = "Le tarif est obligatoire")
    @DecimalMin(value = "0.01", message = "Le tarif doit être supérieur à 0")
    @Digits(integer = 10, fraction = 2, message = "Le tarif doit avoir au maximum 2 décimales")
    private BigDecimal tarif;

    @NotNull(message = "La durée estimée est obligatoire")
    @Positive(message = "La durée estimée doit être positive")
    private Integer dureeEstimee;

    private String unite;

    private BigDecimal limiteMin;

    private BigDecimal limiteMax;

    private BigDecimal seuilCritiqueMin;

    private BigDecimal seuilCritiqueMax;

    @NotNull(message = "Le domaine est obligatoire")
    private Long domaineId;

    private List<@Valid CreerExigenceRequest> exigences;
}
