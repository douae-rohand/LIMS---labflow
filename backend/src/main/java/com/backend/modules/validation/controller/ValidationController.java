package com.backend.modules.validation.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.modules.validation.dto.ValidationDto;
import com.backend.modules.validation.entity.StatutValidation;
import com.backend.modules.validation.service.ValidationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/validations") @RequiredArgsConstructor
@Tag(name = "Validation", description = "Validation des résultats d'essais (M06)")
public class ValidationController {

    private final ValidationService validationService;

    @GetMapping("/essai/{essaiId}")
    public ResponseEntity<ApiResponse<List<ValidationDto>>> listerParEssai(@PathVariable Long essaiId) {
        return ResponseEntity.ok(ApiResponse.success(validationService.listerParEssai(essaiId)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('RESPONSABLE','ADMINISTRATEUR')")
    public ResponseEntity<ApiResponse<ValidationDto>> creer(@RequestBody ValidationDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Validation créée", validationService.creer(dto)));
    }

    @PatchMapping("/{id}/decider")
    @PreAuthorize("hasAnyRole('RESPONSABLE','ADMINISTRATEUR')")
    public ResponseEntity<ApiResponse<ValidationDto>> decider(
            @PathVariable Long id, @RequestParam Long validateurId,
            @RequestParam StatutValidation decision, @RequestParam(required = false) String commentaire) {
        return ResponseEntity.ok(ApiResponse.success("Décision enregistrée",
                validationService.valider(id, validateurId, commentaire, decision)));
    }
}
