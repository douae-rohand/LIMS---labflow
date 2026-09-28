package com.backend.modules.satisfaction.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.modules.satisfaction.dto.SatisfactionDto;
import com.backend.modules.satisfaction.service.SatisfactionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/satisfaction") @RequiredArgsConstructor
@Tag(name = "Satisfaction", description = "Enquêtes de satisfaction client (M10)")
public class SatisfactionController {

    private final SatisfactionService satisfactionService;

    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ApiResponse<SatisfactionDto>> soumettre(@Valid @RequestBody SatisfactionDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Évaluation enregistrée", satisfactionService.soumettre(dto)));
    }

    @GetMapping("/stats/moyenne")
    @PreAuthorize("hasAnyRole('RESPONSABLE','ADMINISTRATEUR')")
    public ResponseEntity<ApiResponse<Double>> moyenneGlobale() {
        return ResponseEntity.ok(ApiResponse.success(satisfactionService.moyenneGlobale()));
    }
}
