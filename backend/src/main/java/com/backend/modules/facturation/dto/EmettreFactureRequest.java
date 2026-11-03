package com.backend.modules.facturation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmettreFactureRequest {
    @NotNull(message = "L'ID de la demande est obligatoire")
    private Long demandeId;

    private LocalDate dateEcheance;
}
