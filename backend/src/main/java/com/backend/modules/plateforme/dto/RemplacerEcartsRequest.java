package com.backend.modules.plateforme.dto;

import com.backend.modules.utilisateur.entity.EffetPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RemplacerEcartsRequest {

    @NotNull
    @Valid
    private List<Ecart> ecarts = new ArrayList<>();

    @Data
    public static class Ecart {
        @NotBlank
        private String code;
        @NotNull
        private EffetPermission effet;
    }
}
