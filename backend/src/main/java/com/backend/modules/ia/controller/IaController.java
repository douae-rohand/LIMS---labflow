package com.backend.modules.ia.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.modules.ia.dto.AnalyseIaRequest;
import com.backend.modules.ia.dto.AnalyseIaResponse;
import com.backend.modules.ia.service.IaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ia")
@RequiredArgsConstructor
@Tag(name = "Intelligence Artificielle", description = "Analyse IA et assistance LLM (M14)")
public class IaController {

    private final IaService iaService;

    @PostMapping("/analyser")
    @PreAuthorize("hasAnyRole('TECHNICIEN','RESPONSABLE','ADMINISTRATEUR')")
    @Operation(summary = "Soumettre une tâche d'analyse au modèle IA")
    public ResponseEntity<ApiResponse<AnalyseIaResponse>> analyser(@RequestBody AnalyseIaRequest request) {
        return ResponseEntity.ok(ApiResponse.success(iaService.analyser(request)));
    }
}
