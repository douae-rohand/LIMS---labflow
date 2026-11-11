package com.backend.modules.facturation.controller;

import com.backend.common.exception.ResourceNotFoundException;
import com.backend.common.security.SecurityUtils;
import com.backend.common.tenant.TenantExecutor;
import com.backend.modules.facturation.service.FacturationClientService;
import com.backend.modules.facturation.service.FacturePdfService;
import com.backend.modules.facturation.service.FacturePdfService.FichierPdf;
import com.backend.modules.plateforme.entity.Laboratoire;
import com.backend.modules.plateforme.repository.LaboratoireRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Facture PDF et avoir PDF : administrateur, accueil (caisse) et client propriétaire. */
@RestController
@RequiredArgsConstructor
@Tag(name = "Facturation — documents", description = "Facture et avoir au format PDF")
public class FactureDocumentsController {

    private final FacturePdfService facturePdfService;
    private final FacturationClientService facturationClientService;
    private final LaboratoireRepository laboratoireRepository;
    private final TenantExecutor tenantExecutor;

    @GetMapping("/api/factures-admin/{id}/pdf")
    @PreAuthorize("hasAuthority('PERM_FACTURATION_CONSULTER')")
    @Operation(summary = "Facture PDF (administrateur)")
    public ResponseEntity<byte[]> factureAdmin(@PathVariable Long id) {
        return pdf(facturePdfService.facture(id));
    }

    @GetMapping("/api/factures-admin/{id}/avoir")
    @PreAuthorize("hasAuthority('PERM_FACTURATION_CONSULTER')")
    @Operation(summary = "Avoir PDF d'une facture annulée (administrateur)")
    public ResponseEntity<byte[]> avoirAdmin(@PathVariable Long id) {
        return pdf(facturePdfService.avoir(id));
    }

    @GetMapping("/api/accueil/caisse/factures/{id}/pdf")
    @PreAuthorize("hasAuthority('PERM_FACTURATION_PAYER')")
    @Operation(summary = "Facture PDF remise au guichet")
    public ResponseEntity<byte[]> factureCaisse(@PathVariable Long id) {
        return pdf(facturePdfService.facture(id));
    }

    @GetMapping("/api/accueil/caisse/factures/{id}/avoir")
    @PreAuthorize("hasAuthority('PERM_FACTURATION_PAYER')")
    @Operation(summary = "Avoir PDF remis au guichet")
    public ResponseEntity<byte[]> avoirCaisse(@PathVariable Long id) {
        return pdf(facturePdfService.avoir(id));
    }

    @GetMapping("/api/client/factures/{laboratoireId}/{factureId}/pdf")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Facture PDF du client (uniquement ses propres factures)")
    public ResponseEntity<byte[]> factureClient(@PathVariable Long laboratoireId, @PathVariable Long factureId) {
        Long moi = SecurityUtils.principalCourant().getId();
        // Contrôle de propriété : lève une 404 si la facture n'appartient pas au client.
        facturationClientService.obtenirDetailClient(moi, laboratoireId, factureId);
        Laboratoire laboratoire = laboratoireRepository.findById(laboratoireId)
                .orElseThrow(() -> new ResourceNotFoundException("Laboratoire", "id", laboratoireId));
        return pdf(tenantExecutor.inTenant(laboratoire.getNomSchema(), () -> facturePdfService.facture(factureId)));
    }

    private static ResponseEntity<byte[]> pdf(FichierPdf fichier) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fichier.nom() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(fichier.contenu());
    }
}
