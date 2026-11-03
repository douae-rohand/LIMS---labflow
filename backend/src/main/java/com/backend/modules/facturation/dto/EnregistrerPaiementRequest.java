package com.backend.modules.facturation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnregistrerPaiementRequest {
    @NotBlank(message = "Le mode de paiement est obligatoire")
    @Pattern(regexp = "ESPECES|VIREMENT|CHEQUE|CARTE", message = "Mode de paiement invalide")
    private String mode;

    @Size(max = 255, message = "La référence ne peut pas dépasser 255 caractères")
    private String reference;
}
