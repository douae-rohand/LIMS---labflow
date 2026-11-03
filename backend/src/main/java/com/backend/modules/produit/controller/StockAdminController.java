package com.backend.modules.produit.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.modules.produit.dto.*;
import com.backend.modules.produit.service.StockAdminService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stock-admin")
@RequiredArgsConstructor
@Tag(name = "Stock Admin", description = "Gestion du stock pour les administrateurs (M09)")
public class StockAdminController {

    private final StockAdminService stockAdminService;

    @GetMapping("/produits")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<ApiResponse<Page<ProduitDto>>> listerProduits(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(stockAdminService.listerProduits(PageRequest.of(page, size))));
    }

    @GetMapping("/produits/{id}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<ApiResponse<ProduitDto>> getProduit(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(stockAdminService.getProduit(id)));
    }

    @PostMapping("/produits")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<ApiResponse<ProduitDto>> creerProduit(@Valid @RequestBody CreerProduitRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Produit créé", stockAdminService.creerProduit(request)));
    }

    @PutMapping("/produits/{id}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<ApiResponse<ProduitDto>> modifierProduit(
            @PathVariable Long id,
            @Valid @RequestBody ModifierProduitRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Produit modifié", stockAdminService.modifierProduit(id, request)));
    }

    @DeleteMapping("/produits/{id}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<ApiResponse<Void>> supprimerProduit(@PathVariable Long id) {
        stockAdminService.supprimerProduit(id);
        return ResponseEntity.ok(ApiResponse.success("Produit supprimé", null));
    }

    @GetMapping("/produits/{produitId}/lots")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<ApiResponse<java.util.List<LotDto>>> listerLots(@PathVariable Long produitId) {
        return ResponseEntity.ok(ApiResponse.success(stockAdminService.listerLotsParProduit(produitId)));
    }

    @PostMapping("/lots")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<ApiResponse<LotDto>> creerLot(@Valid @RequestBody CreerLotRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Lot créé", stockAdminService.creerLot(request)));
    }

    @PatchMapping("/lots/{lotId}/ajuster")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<ApiResponse<LotDto>> ajusterStock(
            @PathVariable Long lotId,
            @Valid @RequestBody AjusterStockRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Stock ajusté", stockAdminService.ajusterStock(lotId, request)));
    }

    @GetMapping("/alertes")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<ApiResponse<Map<String, List<?>>>> getAlertes() {
        return ResponseEntity.ok(ApiResponse.success(stockAdminService.getAlertes()));
    }
}
