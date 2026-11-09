package com.backend.modules.rapport.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.modules.rapport.dto.RapportDto;
import com.backend.modules.rapport.dto.RapportTravailDto;
import com.backend.modules.rapport.service.RapportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rapports")
@RequiredArgsConstructor
@Tag(name = "Rapports", description = "Génération, signature et libération des rapports d'analyse (M07)")
public class RapportController {

    private final RapportService rapportService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RapportDto>> trouverParId(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(rapportService.trouverParId(id)));
    }

    @GetMapping("/demande/{demandeId}")
    public ResponseEntity<ApiResponse<Page<RapportDto>>> listerParDemande(
            @PathVariable Long demandeId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(rapportService.listerParDemande(demandeId, PageRequest.of(page, size))));
    }

    @GetMapping("/travail")
    @PreAuthorize("hasAuthority('PERM_RAPPORTS_GERER')")
    @Operation(summary = "Demandes terminées avec leur rapport (à générer, à signer, libéré)")
    public ResponseEntity<ApiResponse<List<RapportTravailDto>>> travail() {
        return ResponseEntity.ok(ApiResponse.success(rapportService.travail()));
    }

    @PostMapping("/generer/{demandeId}")
    @PreAuthorize("hasAuthority('PERM_RAPPORTS_GERER')")
    @Operation(summary = "Générer le projet de rapport d'une demande terminée")
    public ResponseEntity<ApiResponse<RapportDto>> generer(@PathVariable Long demandeId) {
        return ResponseEntity.ok(ApiResponse.success("Rapport généré", rapportService.generer(demandeId)));
    }

    @GetMapping("/{id}/apercu")
    @PreAuthorize("hasAuthority('PERM_RAPPORTS_GERER')")
    @Operation(summary = "Aperçu PDF du projet de rapport (non signé)")
    public ResponseEntity<byte[]> apercu(@PathVariable Long id) {
        return pdf("apercu-rapport-" + id + ".pdf", rapportService.apercu(id), false);
    }

    @PostMapping("/{id}/signer")
    @PreAuthorize("hasRole('RESPONSABLE') and hasAuthority('PERM_RAPPORTS_GERER')")
    @Operation(summary = "Signer et libérer le rapport (visible du client)")
    public ResponseEntity<ApiResponse<RapportTravailDto>> signer(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Rapport signé et libéré", rapportService.signerEtLiberer(id)));
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAuthority('PERM_RAPPORTS_GERER')")
    @Operation(summary = "Télécharger le PDF signé")
    public ResponseEntity<byte[]> telecharger(@PathVariable Long id) {
        RapportService.FichierRapport fichier = rapportService.telecharger(id);
        return pdf(fichier.nom(), fichier.contenu(), true);
    }

    private static ResponseEntity<byte[]> pdf(String nom, byte[] contenu, boolean telechargement) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        (telechargement ? "attachment" : "inline") + "; filename=\"" + nom.replace("\"", "") + "\"")
                .body(contenu);
    }
}
