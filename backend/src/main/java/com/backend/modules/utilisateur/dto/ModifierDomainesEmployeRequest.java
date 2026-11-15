package com.backend.modules.utilisateur.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ModifierDomainesEmployeRequest {

    @NotNull
    @Size(max = 50)
    private List<@NotNull Long> domaineIds;
}
