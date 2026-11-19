package com.backend.modules.ia.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.common.security.SecurityUtils;
import com.backend.modules.ia.dto.AssistantDtos.EtatIndex;
import com.backend.modules.ia.dto.AssistantDtos.QuestionRequest;
import com.backend.modules.ia.dto.AssistantDtos.ReponseAssistant;
import com.backend.modules.ia.rag.AssistantService;
import com.backend.modules.ia.rag.IndexationRagService;
import com.backend.modules.ia.rag.ProfilsAssistantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Assistant IA (M14).
 *
 * <ul>
 *   <li>{@code POST /api/ia/assistant} : tout utilisateur connecté ; le profil (rôle, laboratoires)
 *       est déduit du jeton, jamais du corps de la requête.</li>
 *   <li>{@code POST /api/public/ia/assistant} : visiteurs des pages publiques, limités par adresse IP.</li>
 *   <li>{@code POST /api/ia/assistant/reindexer} : Super Admin, reconstruit l'index.</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Intelligence Artificielle", description = "Assistant IA de LabFlow (M14)")
public class IaController {

    private final AssistantService assistantService;
    private final ProfilsAssistantService profils;
    private final IndexationRagService indexationRagService;

    @PostMapping("/api/ia/assistant")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Poser une question à l'assistant (utilisateur connecté, tous rôles)")
    public ResponseEntity<ApiResponse<ReponseAssistant>> poserQuestion(@Valid @RequestBody QuestionRequest requete) {
        var profil = profils.utilisateur(SecurityUtils.principalCourant());
        return ResponseEntity.ok(ApiResponse.success(assistantService.repondre(profil, requete)));
    }

    @PostMapping("/api/public/ia/assistant")
    @Operation(summary = "Poser une question à l'assistant depuis les pages publiques (sans connexion)")
    public ResponseEntity<ApiResponse<ReponseAssistant>> poserQuestionPublique(
            @Valid @RequestBody QuestionRequest requete, HttpServletRequest http) {
        var profil = profils.visiteur(http.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success(assistantService.repondre(profil, requete)));
    }

    @PostMapping("/api/ia/assistant/reindexer")
    @PreAuthorize("hasRole('SUPER_ADMINISTRATEUR')")
    @Operation(summary = "Reconstruire l'index de l'assistant (documentation et catalogues)")
    public ResponseEntity<ApiResponse<EtatIndex>> reindexer() {
        var bilan = indexationRagService.reindexerTout();
        return ResponseEntity.ok(ApiResponse.success(new EtatIndex(indexationRagService.estPret(), bilan)));
    }
}
