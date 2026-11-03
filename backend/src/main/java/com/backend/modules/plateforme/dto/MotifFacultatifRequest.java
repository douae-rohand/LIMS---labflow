package com.backend.modules.plateforme.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MotifFacultatifRequest {

    @Size(max = 2000, message = "Le motif ne peut pas dépasser 2000 caractères")
    private String motif;
}
