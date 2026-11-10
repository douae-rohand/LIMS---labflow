package com.backend.modules.validation.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.modules.planification.dto.LigneTravailDto;
import com.backend.modules.validation.dto.DecisionValidationRequest;
import com.backend.modules.validation.dto.ValidationDto;
import com.backend.modules.validation.dto.ValidationTechniqueRequest;
import com.backend.modules.validation.service.ValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/validations")
@RequiredArgsConstructor
@Tag(name = "Validations", description = "Validation des résultats à deux niveaux (M06)")
public class ValidationController {

    private final ValidationService validationService;

    @GetMapping("/essai/{essaiId}")
    @Operation(summary = "Historique des validations d'une analyse")
    public ResponseEntity<ApiResponse<List<ValidationDto>>> listerParEssai(@PathVariable Long essaiId) {
        return ResponseEntity.ok(ApiResponse.success(validationService.listerParEssai(essaiId)));
    }

    @GetMapping("/a-valider")
    @PreAuthorize("hasAuthority('PERM_VALIDATION_GERER')")
    @Operation(summary = "Résultats validés techniquement, en attente de la décision du responsable")
    public ResponseEntity<ApiResponse<List<LigneTravailDto>>> aValider() {
        return ResponseEntity.ok(ApiResponse.success(validationService.resultatsAValider()));
    }

    @PostMapping("/essai/{essaiId}/technique")
    @PreAuthorize("hasAuthority('PERM_ESSAIS_GERER')")
    @Operation(summary = "Niveau 1 : validation technique par le technicien affecté")
    public ResponseEntity<ApiResponse<ValidationDto>> validerTechniquement(
            @PathVariable Long essaiId,
            @Valid @RequestBody(required = false) ValidationTechniqueRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Résultat validé techniquement",
                validationService.validerTechniquement(essaiId, request == null ? null : request.commentaire())));
    }

    @PostMapping("/essai/{essaiId}/decision")
    @PreAuthorize("hasRole('RESPONSABLE') and hasAuthority('PERM_VALIDATION_GERER')")
    @Operation(summary = "Niveau 2 : valider, retourner ou rejeter un résultat")
    public ResponseEntity<ApiResponse<LigneTravailDto>> decider(
            @PathVariable Long essaiId,
            @Valid @RequestBody DecisionValidationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Décision enregistrée",
                validationService.decider(essaiId, request)));
    }
}
