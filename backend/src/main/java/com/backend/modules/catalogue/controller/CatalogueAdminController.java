package com.backend.modules.catalogue.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.common.dto.PageResponse;
import com.backend.modules.catalogue.dto.*;
import com.backend.modules.catalogue.service.CatalogueAdminService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalogue-admin")
@RequiredArgsConstructor
@Tag(name = "Catalogue Admin", description = "Gestion du catalogue pour les administrateurs (M04)")
public class CatalogueAdminController {

    private final CatalogueAdminService catalogueAdminService;

    @GetMapping("/essais")
    @PreAuthorize("hasAuthority('PERM_CATALOGUE_CONSULTER')")
    public ResponseEntity<ApiResponse<PageResponse<EssaiAdminDto>>> listerEssais(
            @RequestParam(required = false) Long domaineId,
            @RequestParam(required = false) Boolean actif,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        org.springframework.data.domain.Page<EssaiAdminDto> pageResult = 
                catalogueAdminService.listerEssais(domaineId, actif, q, 
                    PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(pageResult)));
    }

    @GetMapping("/essais/{id}")
    @PreAuthorize("hasAuthority('PERM_CATALOGUE_CONSULTER')")
    public ResponseEntity<ApiResponse<EssaiAdminDto>> getEssai(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(catalogueAdminService.getEssai(id)));
    }

    @PostMapping("/essais")
    @PreAuthorize("hasAuthority('PERM_CATALOGUE_GERER')")
    public ResponseEntity<ApiResponse<EssaiAdminDto>> creerEssai(@Valid @RequestBody CreerEssaiRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Essai créé", catalogueAdminService.creerEssai(request)));
    }

    @PutMapping("/essais/{id}")
    @PreAuthorize("hasAuthority('PERM_CATALOGUE_GERER')")
    public ResponseEntity<ApiResponse<EssaiAdminDto>> modifierEssai(
            @PathVariable Long id,
            @Valid @RequestBody ModifierEssaiRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Essai modifié", catalogueAdminService.modifierEssai(id, request)));
    }

    @PatchMapping("/essais/{id}/activer")
    @PreAuthorize("hasAuthority('PERM_CATALOGUE_GERER')")
    public ResponseEntity<ApiResponse<EssaiAdminDto>> activerEssai(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Essai activé", catalogueAdminService.activerEssai(id)));
    }

    @PatchMapping("/essais/{id}/desactiver")
    @PreAuthorize("hasAuthority('PERM_CATALOGUE_GERER')")
    public ResponseEntity<ApiResponse<EssaiAdminDto>> desactiverEssai(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Essai désactivé", catalogueAdminService.desactiverEssai(id)));
    }

    @DeleteMapping("/essais/{id}")
    @PreAuthorize("hasAuthority('PERM_CATALOGUE_GERER')")
    public ResponseEntity<ApiResponse<Void>> supprimerEssai(@PathVariable Long id) {
        catalogueAdminService.supprimerEssai(id);
        return ResponseEntity.ok(ApiResponse.success("Essai supprimé", null));
    }

    @GetMapping("/domaines")
    @PreAuthorize("hasAuthority('PERM_CATALOGUE_CONSULTER')")
    public ResponseEntity<ApiResponse<List<DomaineDto>>> listerDomaines() {
        return ResponseEntity.ok(ApiResponse.success(catalogueAdminService.listerDomaines()));
    }

    @PostMapping("/domaines")
    @PreAuthorize("hasAuthority('PERM_CATALOGUE_GERER')")
    public ResponseEntity<ApiResponse<DomaineDto>> creerDomaine(@Valid @RequestBody CreerDomaineRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Domaine créé", catalogueAdminService.creerDomaine(request)));
    }

    @PutMapping("/domaines/{id}")
    @PreAuthorize("hasAuthority('PERM_CATALOGUE_GERER')")
    public ResponseEntity<ApiResponse<DomaineDto>> modifierDomaine(
            @PathVariable Long id,
            @Valid @RequestBody ModifierDomaineRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Domaine modifié", catalogueAdminService.modifierDomaine(id, request)));
    }

    @DeleteMapping("/domaines/{id}")
    @PreAuthorize("hasAuthority('PERM_CATALOGUE_GERER')")
    public ResponseEntity<ApiResponse<Void>> supprimerDomaine(@PathVariable Long id) {
        catalogueAdminService.supprimerDomaine(id);
        return ResponseEntity.ok(ApiResponse.success("Domaine supprimé", null));
    }

    @GetMapping("/exigences/{essaiId}")
    @PreAuthorize("hasAuthority('PERM_CATALOGUE_CONSULTER')")
    public ResponseEntity<ApiResponse<List<ExigenceEchantillonDto>>> listerExigences(@PathVariable Long essaiId) {
        return ResponseEntity.ok(ApiResponse.success(catalogueAdminService.listerExigences(essaiId)));
    }

    @GetMapping("/exigences/suggestions")
    @PreAuthorize("hasAuthority('PERM_CATALOGUE_CONSULTER')")
    public ResponseEntity<ApiResponse<List<ExigenceEchantillonDto>>> suggestionsExigences() {
        return ResponseEntity.ok(ApiResponse.success(catalogueAdminService.suggestionsExigences()));
    }

    @PostMapping("/exigences")
    @PreAuthorize("hasAuthority('PERM_CATALOGUE_GERER')")
    public ResponseEntity<ApiResponse<ExigenceEchantillonDto>> creerExigence(
            @Valid @RequestBody CreerExigenceRequest request,
            @RequestParam Long essaiId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Exigence créée", catalogueAdminService.creerExigence(request, essaiId)));
    }

    @PutMapping("/exigences/{id}")
    @PreAuthorize("hasAuthority('PERM_CATALOGUE_GERER')")
    public ResponseEntity<ApiResponse<ExigenceEchantillonDto>> modifierExigence(
            @PathVariable Long id,
            @Valid @RequestBody CreerExigenceRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Exigence modifiée", catalogueAdminService.modifierExigence(id, request)));
    }

    @DeleteMapping("/exigences/{id}")
    @PreAuthorize("hasAuthority('PERM_CATALOGUE_GERER')")
    public ResponseEntity<ApiResponse<Void>> supprimerExigence(@PathVariable Long id) {
        catalogueAdminService.supprimerExigence(id);
        return ResponseEntity.ok(ApiResponse.success("Exigence supprimée", null));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('PERM_CATALOGUE_CONSULTER')")
    public ResponseEntity<ApiResponse<CatalogueStatsDto>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(catalogueAdminService.getStats()));
    }
}
