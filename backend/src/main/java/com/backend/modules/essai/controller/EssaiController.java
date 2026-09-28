package com.backend.modules.essai.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.modules.essai.dto.EssaiDto;
import com.backend.modules.essai.service.EssaiService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/essais") @RequiredArgsConstructor
@Tag(name = "Essais", description = "Réalisation des essais (M05)")
public class EssaiController {

    private final EssaiService essaiService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EssaiDto>> trouverParId(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(essaiService.trouverParId(id)));
    }

    @GetMapping("/echantillon/{echantillonId}")
    public ResponseEntity<ApiResponse<List<EssaiDto>>> listerParEchantillon(@PathVariable Long echantillonId) {
        return ResponseEntity.ok(ApiResponse.success(essaiService.listerParEchantillon(echantillonId)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TECHNICIEN','RESPONSABLE')")
    public ResponseEntity<ApiResponse<EssaiDto>> creer(@RequestBody EssaiDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Essai créé", essaiService.creer(dto)));
    }

    @PatchMapping("/{id}/resultat")
    @PreAuthorize("hasAnyRole('TECHNICIEN','RESPONSABLE')")
    public ResponseEntity<ApiResponse<EssaiDto>> saisirResultat(@PathVariable Long id, @RequestBody EssaiDto dto) {
        return ResponseEntity.ok(ApiResponse.success("Résultat enregistré", essaiService.saisirResultat(id, dto)));
    }
}
