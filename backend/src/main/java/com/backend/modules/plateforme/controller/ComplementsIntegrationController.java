package com.backend.modules.plateforme.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.modules.plateforme.dto.ComplementsPublicDto;
import com.backend.modules.plateforme.service.DemandeIntegrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Accès public du demandeur à sa demande de compléments, par le lien reçu par e-mail.
 * Le jeton circule dans le corps des requêtes, jamais dans l'URL de l'API.
 */
@RestController
@RequestMapping("/api/public/integration/complements")
@RequiredArgsConstructor
@Tag(name = "Intégration publique", description = "Réponse du demandeur à une demande de compléments")
public class ComplementsIntegrationController {

    private final DemandeIntegrationService demandeIntegrationService;

    public record JetonRequest(String jeton) {
    }

    @PostMapping("/consulter")
    @Operation(summary = "Lire la demande de compléments associée au jeton")
    public ResponseEntity<ApiResponse<ComplementsPublicDto>> consulter(@RequestBody JetonRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                demandeIntegrationService.consulterComplements(request == null ? null : request.jeton())));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Déposer les documents demandés et/ou répondre au message")
    public ResponseEntity<ApiResponse<ComplementsPublicDto>> deposer(
            @RequestParam("jeton") String jeton,
            @RequestParam(value = "reponse", required = false) String reponse,
            MultipartHttpServletRequest requete) {
        Map<String, MultipartFile> fichiers = new LinkedHashMap<>(requete.getFileMap());
        return ResponseEntity.ok(ApiResponse.success("Compléments transmis",
                demandeIntegrationService.deposerComplements(jeton, reponse, fichiers)));
    }
}
