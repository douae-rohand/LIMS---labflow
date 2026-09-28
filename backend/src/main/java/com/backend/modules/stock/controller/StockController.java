package com.backend.modules.stock.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.modules.stock.dto.ArticleStockDto;
import com.backend.modules.stock.entity.TypeMouvement;
import com.backend.modules.stock.service.StockService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/stock") @RequiredArgsConstructor
@Tag(name = "Stock", description = "Gestion des stocks de réactifs et consommables (M09)")
public class StockController {

    private final StockService stockService;

    @GetMapping
    @PreAuthorize("hasAnyRole('TECHNICIEN','RESPONSABLE','ADMINISTRATEUR')")
    public ResponseEntity<ApiResponse<Page<ArticleStockDto>>> lister(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.success(stockService.lister(PageRequest.of(page, size))));
    }

    @GetMapping("/alertes")
    @PreAuthorize("hasAnyRole('TECHNICIEN','RESPONSABLE','ADMINISTRATEUR')")
    public ResponseEntity<ApiResponse<List<ArticleStockDto>>> alertesRupture() {
        return ResponseEntity.ok(ApiResponse.success(stockService.alertesRupture()));
    }

    @PostMapping("/{articleId}/mouvement")
    @PreAuthorize("hasAnyRole('TECHNICIEN','RESPONSABLE','ADMINISTRATEUR')")
    public ResponseEntity<ApiResponse<ArticleStockDto>> enregistrerMouvement(
            @PathVariable Long articleId, @RequestParam TypeMouvement type,
            @RequestParam Double quantite, @RequestParam(required = false) String motif,
            @RequestParam Long operateurId) {
        return ResponseEntity.ok(ApiResponse.success("Mouvement enregistré",
                stockService.enregistrerMouvement(articleId, type, quantite, motif, operateurId)));
    }
}
