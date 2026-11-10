package com.backend.modules.plateforme.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.modules.plateforme.dto.FichierGenere;
import com.backend.modules.plateforme.service.FinancesLaboratoireService;
import com.backend.modules.plateforme.service.FinancesLaboratoireService.FinancesLaboratoire;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/plateforme/laboratoires/{id}/finances")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMINISTRATEUR')")
@Tag(name = "Plateforme — Finances", description = "Montants facturés et encaissés par laboratoire")
public class FinancesLaboratoireController {

    private final FinancesLaboratoireService financesLaboratoireService;

    @GetMapping
    @Operation(summary = "Activité financière d'un laboratoire sur une période, par jour, semaine ou mois")
    public ResponseEntity<ApiResponse<FinancesLaboratoire>> synthese(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
            @RequestParam(required = false) String regroupement) {
        return ResponseEntity.ok(ApiResponse.success(financesLaboratoireService.synthese(id, du, au, regroupement)));
    }

    @GetMapping("/export")
    @Operation(summary = "Exporter l'activité financière en PDF, Excel ou CSV")
    public ResponseEntity<byte[]> exporter(
            @PathVariable Long id,
            @RequestParam String format,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
            @RequestParam(required = false) String regroupement) {
        FichierGenere fichier = financesLaboratoireService.exporter(id, du, au, regroupement, format);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fichier.nom() + "\"")
                .contentType(MediaType.parseMediaType(fichier.typeMime()))
                .body(fichier.contenu());
    }
}
