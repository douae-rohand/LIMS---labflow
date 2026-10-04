package com.backend.modules.client.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.common.security.SecurityUtils;
import com.backend.modules.client.dto.ClientProfilDto;
import com.backend.modules.client.dto.ModifierClientProfilRequest;
import com.backend.modules.client.service.ClientService;
import com.backend.modules.demande.dto.DemandeDto;
import com.backend.modules.demande.service.DemandeService;
import com.backend.modules.facturation.dto.FactureDto;
import com.backend.modules.facturation.service.FacturationService;
import com.backend.modules.plateforme.dto.LaboratoirePublicDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/client")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLIENT')")
@Tag(name = "Client", description = "Espace client multi-laboratoires")
public class ClientController {

    private final ClientService clientService;
    private final DemandeService demandeService;
    private final FacturationService facturationService;

    @GetMapping("/profil")
    public ResponseEntity<ApiResponse<ClientProfilDto>> lireProfil() {
        return ResponseEntity.ok(ApiResponse.success(
                clientService.lireProfil(SecurityUtils.principalCourant().getId())));
    }

    @PatchMapping("/profil")
    public ResponseEntity<ApiResponse<ClientProfilDto>> modifierProfil(
            @RequestBody ModifierClientProfilRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Profil mis à jour",
                clientService.modifierProfil(SecurityUtils.principalCourant().getId(), request)));
    }

    @GetMapping("/laboratoires")
    public ResponseEntity<ApiResponse<List<LaboratoirePublicDto>>> mesLaboratoires() {
        return ResponseEntity.ok(ApiResponse.success(
                clientService.listerLaboratoiresDuClient(SecurityUtils.principalCourant().getId())));
    }

    @GetMapping("/demandes")
    public ResponseEntity<ApiResponse<List<DemandeDto>>> mesDemandes() {
        return ResponseEntity.ok(ApiResponse.success(demandeService.listerToutesMesDemandes()));
    }

    @GetMapping("/factures")
    public ResponseEntity<ApiResponse<List<FactureDto>>> mesFactures() {
        return ResponseEntity.ok(ApiResponse.success(facturationService.listerToutesMesFactures()));
    }
}
