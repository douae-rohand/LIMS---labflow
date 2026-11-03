package com.backend.modules.produit.dto;

import com.backend.modules.stock.entity.TypeMouvement;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AjusterStockRequest {
    @NotNull(message = "L'ID du lot est obligatoire")
    private Long lotId;

    @NotNull(message = "Le type de mouvement est obligatoire")
    private TypeMouvement type;

    @NotNull(message = "La quantité est obligatoire")
    private BigDecimal quantite;

    @NotBlank(message = "Le motif est obligatoire")
    private String motif;
}
