package com.backend.modules.plateforme.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.modules.plateforme.dto.LaboratoirePublicDto;
import com.backend.modules.plateforme.service.PlateformeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/laboratoires")
@RequiredArgsConstructor
@Tag(name = "Laboratoires", description = "Catalogue public des laboratoires actifs")
public class LaboratoirePublicController {

    private final PlateformeService plateformeService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<LaboratoirePublicDto>>> listerActifs() {
        return ResponseEntity.ok(ApiResponse.success(plateformeService.listerLaboratoiresPublics()));
    }

    @GetMapping("/{code}")
    public ResponseEntity<ApiResponse<LaboratoirePublicDto>> trouver(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.success(plateformeService.trouverPublicParCode(code)));
    }
}
