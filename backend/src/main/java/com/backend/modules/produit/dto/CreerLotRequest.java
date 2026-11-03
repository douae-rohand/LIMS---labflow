package com.backend.modules.produit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreerLotRequest {
    @NotNull(message = "L'ID du produit est obligatoire")
    private Long produitId;

    @NotBlank(message = "Le numéro est obligatoire")
    private String numero;

    @NotNull(message = "La quantité est obligatoire")
    @Positive(message = "La quantité doit être positive")
    private BigDecimal quantite;

    @NotNull(message = "La date de péremption est obligatoire")
    private LocalDate peremption;
}
