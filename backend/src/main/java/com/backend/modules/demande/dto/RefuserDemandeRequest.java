package com.backend.modules.demande.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefuserDemandeRequest {

    @NotBlank(message = "Le motif du refus est obligatoire.")
    @Size(max = 2000, message = "Le motif ne peut pas dépasser 2000 caractères.")
    private String motif;
}
