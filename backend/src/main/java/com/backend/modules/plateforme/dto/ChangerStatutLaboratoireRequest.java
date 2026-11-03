package com.backend.modules.plateforme.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangerStatutLaboratoireRequest {

    @NotBlank(message = "Le motif est obligatoire")
    @Size(min = 10, max = 2000, message = "Le motif doit contenir entre 10 et 2000 caractères")
    private String motif;
}
