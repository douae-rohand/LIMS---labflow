package com.backend.modules.utilisateur.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.common.dto.PageResponse;
import com.backend.modules.utilisateur.dto.EmployeDto;
import com.backend.modules.utilisateur.service.UtilisateurService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints dédiés à la gestion des employés de laboratoire.
 * Base path : {@code /api/employes}
 */
@RestController
@RequestMapping("/api/employes")
@RequiredArgsConstructor
@Tag(name = "Employés", description = "Gestion des employés du laboratoire courant (réservé à l'Administrateur)")
public class EmployeController {

    private final UtilisateurService utilisateurService;

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @Operation(summary = "Lister les employés du laboratoire courant avec pagination, filtres et recherche")
    public ResponseEntity<ApiResponse<PageResponse<EmployeDto>>> listerEmployes(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "nom") String tri,
            @RequestParam(defaultValue = "asc") String direction) {

        PageResponse<EmployeDto> response = utilisateurService.listerEmployes(
                q, statut, role, page, size, tri, direction
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @Operation(summary = "Obtenir le détail d'un employé du laboratoire courant")
    public ResponseEntity<ApiResponse<com.backend.modules.utilisateur.dto.EmployeDetailDto>> trouverEmployeParId(
            @org.springframework.web.bind.annotation.PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(utilisateurService.trouverEmployeParId(id)));
    }

    @org.springframework.web.bind.annotation.PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @Operation(summary = "Modifier un employé du laboratoire courant")
    public ResponseEntity<ApiResponse<com.backend.modules.utilisateur.dto.EmployeDetailDto>> modifierEmploye(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody com.backend.modules.utilisateur.dto.ModifierEmployeRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Employé modifié avec succès", utilisateurService.modifierEmploye(id, request)));
    }
}
