package com.backend.modules.plateforme.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.modules.plateforme.dto.LaboratoireParametresDto;
import com.backend.modules.plateforme.dto.ModifierLaboratoireParametresRequest;
import com.backend.modules.plateforme.service.LaboratoireParametresService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/laboratoire/parametres")
@RequiredArgsConstructor
@Tag(name = "Paramètres Laboratoire", description = "Paramètres du laboratoire (M04)")
public class LaboratoireParametresController {

    private final LaboratoireParametresService laboratoireParametresService;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_PARAMETRES_LABORATOIRE_GERER')")
    public ResponseEntity<ApiResponse<LaboratoireParametresDto>> getParametres() {
        return ResponseEntity.ok(ApiResponse.success(laboratoireParametresService.getParametres()));
    }

    @PatchMapping
    @PreAuthorize("hasAuthority('PERM_PARAMETRES_LABORATOIRE_GERER')")
    public ResponseEntity<ApiResponse<LaboratoireParametresDto>> modifierParametres(
            @Valid @RequestBody ModifierLaboratoireParametresRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Paramètres modifiés", laboratoireParametresService.modifierParametres(request)));
    }
}
