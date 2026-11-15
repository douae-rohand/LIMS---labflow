package com.backend.modules.accueil.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.modules.accueil.dto.ReceptionDtos.DemandeReceptionDto;
import com.backend.modules.accueil.dto.ReceptionDtos.DetailReceptionDto;
import com.backend.modules.accueil.dto.ReceptionDtos.ReceptionRequest;
import com.backend.modules.accueil.dto.ReceptionDtos.TableauAccueilDto;
import com.backend.modules.accueil.service.ReceptionService;
import com.backend.modules.accueil.service.TableauAccueilService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Espace accueil : tableau de bord et réception des échantillons (M03). */
@RestController
@RequestMapping("/api/accueil")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('PERM_ECHANTILLONS_GERER')")
@Tag(name = "Accueil", description = "Réception des échantillons et tableau de bord de l'accueil")
public class AccueilController {

    private final ReceptionService receptionService;
    private final TableauAccueilService tableauAccueilService;

    @GetMapping("/tableau")
    @Operation(summary = "Tableau de bord de l'accueil")
    public ResponseEntity<ApiResponse<TableauAccueilDto>> tableau() {
        return ResponseEntity.ok(ApiResponse.success(tableauAccueilService.tableau()));
    }

    @GetMapping("/receptions")
    @Operation(summary = "Demandes acceptées ou en cours et leur état de réception")
    public ResponseEntity<ApiResponse<List<DemandeReceptionDto>>> receptions() {
        return ResponseEntity.ok(ApiResponse.success(receptionService.lister()));
    }

    @GetMapping("/receptions/{demandeId}")
    @Operation(summary = "Analyses, échantillons attendus et échantillons reçus d'une demande")
    public ResponseEntity<ApiResponse<DetailReceptionDto>> detail(@PathVariable Long demandeId) {
        return ResponseEntity.ok(ApiResponse.success(receptionService.detail(demandeId)));
    }

    @GetMapping("/receptions/{demandeId}/etiquettes")
    @Operation(summary = "Étiquettes code-barres (Code 128) des échantillons reçus, au format PDF")
    public ResponseEntity<byte[]> etiquettes(
            @PathVariable Long demandeId,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Long echantillonId,
            @org.springframework.web.bind.annotation.RequestParam(required = false)
            com.backend.modules.accueil.service.EtiquettesPdfService.Format format) {
        com.backend.modules.accueil.service.CaisseService.FichierRecu fichier =
                receptionService.etiquettes(demandeId, echantillonId, format);
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        org.springframework.http.ContentDisposition.inline().filename(fichier.nom()).build().toString())
                .body(fichier.contenu());
    }

    @GetMapping("/receptions/{demandeId}/echantillons/{echantillonId}/code-barres")
    @Operation(summary = "Code-barres Code 128 d'un échantillon (PNG), pour l'aperçu de l'étiquette")
    public ResponseEntity<byte[]> codeBarres(@PathVariable Long demandeId, @PathVariable Long echantillonId) {
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.IMAGE_PNG)
                .header(org.springframework.http.HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                .body(receptionService.codeBarres(demandeId, echantillonId));
    }

    @PostMapping("/receptions/{demandeId}")
    @Operation(summary = "Réceptionner un ou plusieurs échantillons (réception partielle possible)")
    public ResponseEntity<ApiResponse<DetailReceptionDto>> receptionner(@PathVariable Long demandeId,
                                                                        @Valid @RequestBody ReceptionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Réception enregistrée",
                receptionService.receptionner(demandeId, request)));
    }
}
