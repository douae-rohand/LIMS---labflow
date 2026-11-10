package com.backend.modules.plateforme.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.common.security.SecurityUtils;
import com.backend.modules.plateforme.dto.ParametresPlateformeDtos.CreerTypeLaboratoire;
import com.backend.modules.plateforme.dto.ParametresPlateformeDtos.EnregistrerTypeDocument;
import com.backend.modules.plateforme.dto.ParametresPlateformeDtos.ModifierTypeLaboratoire;
import com.backend.modules.plateforme.dto.ParametresPlateformeDtos.Securite;
import com.backend.modules.plateforme.dto.ParametresPlateformeDtos.TypeLaboratoire;
import com.backend.modules.plateforme.dto.TypeDocumentIntegrationDto;
import com.backend.modules.plateforme.service.ParametrePlateformeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Paramètres globaux de la plateforme, réservés au Super Administrateur. */
@RestController
@RequestMapping("/api/plateforme/parametres")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMINISTRATEUR')")
@Tag(name = "Paramètres plateforme", description = "Sécurité, documents d'intégration et types de laboratoire")
public class ParametresPlateformeController {

    private final ParametrePlateformeService service;

    @GetMapping("/securite")
    @Operation(summary = "Paramètres de sécurité")
    public ResponseEntity<ApiResponse<Securite>> securite() {
        return ResponseEntity.ok(ApiResponse.success(service.securite()));
    }

    @PutMapping("/securite")
    @Operation(summary = "Modifier les paramètres de sécurité")
    public ResponseEntity<ApiResponse<Securite>> modifierSecurite(@Valid @RequestBody Securite demande) {
        return ResponseEntity.ok(ApiResponse.success("Paramètres de sécurité enregistrés",
                service.modifierSecurite(demande, acteur())));
    }

    @GetMapping("/documents")
    @Operation(summary = "Documents exigés pour l'intégration, actifs et inactifs")
    public ResponseEntity<ApiResponse<List<TypeDocumentIntegrationDto>>> documents() {
        return ResponseEntity.ok(ApiResponse.success(service.listerTypesDocuments()));
    }

    @PostMapping("/documents")
    @Operation(summary = "Ajouter un document exigé")
    public ResponseEntity<ApiResponse<TypeDocumentIntegrationDto>> creerDocument(
            @Valid @RequestBody EnregistrerTypeDocument demande) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Document ajouté",
                service.creerTypeDocument(demande, acteur())));
    }

    @PutMapping("/documents/{code}")
    @Operation(summary = "Modifier un document exigé")
    public ResponseEntity<ApiResponse<TypeDocumentIntegrationDto>> modifierDocument(
            @PathVariable String code, @Valid @RequestBody EnregistrerTypeDocument demande) {
        return ResponseEntity.ok(ApiResponse.success("Document mis à jour",
                service.modifierTypeDocument(code, demande, acteur())));
    }

    @GetMapping("/types-laboratoire")
    @Operation(summary = "Types de laboratoire de référence")
    public ResponseEntity<ApiResponse<List<TypeLaboratoire>>> typesLaboratoire() {
        return ResponseEntity.ok(ApiResponse.success(service.listerTypesLaboratoire()));
    }

    @PostMapping("/types-laboratoire")
    @Operation(summary = "Ajouter un type de laboratoire")
    public ResponseEntity<ApiResponse<TypeLaboratoire>> creerTypeLaboratoire(
            @Valid @RequestBody CreerTypeLaboratoire demande) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Type ajouté",
                service.creerTypeLaboratoire(demande, acteur())));
    }

    @PatchMapping("/types-laboratoire/{id}")
    @Operation(summary = "Activer, désactiver ou réordonner un type de laboratoire")
    public ResponseEntity<ApiResponse<TypeLaboratoire>> modifierTypeLaboratoire(
            @PathVariable Long id, @Valid @RequestBody ModifierTypeLaboratoire demande) {
        return ResponseEntity.ok(ApiResponse.success("Type mis à jour",
                service.modifierTypeLaboratoire(id, demande, acteur())));
    }

    private static String acteur() {
        return SecurityUtils.principalCourant().getEmail();
    }
}
