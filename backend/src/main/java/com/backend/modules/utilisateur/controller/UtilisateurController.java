package com.backend.modules.utilisateur.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.common.dto.PageResponse;
import com.backend.modules.utilisateur.dto.CreerUtilisateurRequest;
import com.backend.modules.utilisateur.dto.ModifierUtilisateurRequest;
import com.backend.modules.utilisateur.dto.UtilisateurDto;
import com.backend.modules.utilisateur.entity.RoleUtilisateur;
import com.backend.modules.utilisateur.service.UtilisateurService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints de gestion des utilisateurs (M12).
 * Base path : {@code /api/utilisateurs}
 */
@RestController
@RequestMapping("/api/utilisateurs")
@RequiredArgsConstructor
@Tag(name = "Utilisateurs", description = "Gestion des comptes utilisateurs (M12)")
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','SUPER_ADMINISTRATEUR')")
    @Operation(summary = "Lister les utilisateurs avec recherche et pagination")
    public ResponseEntity<ApiResponse<PageResponse<UtilisateurDto>>> lister(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("nom").ascending());
        return ResponseEntity.ok(ApiResponse.success(utilisateurService.lister(search, pageable)));
    }

    @GetMapping("/role/{role}")
    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','SUPER_ADMINISTRATEUR')")
    @Operation(summary = "Lister les utilisateurs par rôle")
    public ResponseEntity<ApiResponse<PageResponse<UtilisateurDto>>> listerParRole(
            @PathVariable RoleUtilisateur role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("nom").ascending());
        return ResponseEntity.ok(ApiResponse.success(utilisateurService.listerParRole(role, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','SUPER_ADMINISTRATEUR') or #id == authentication.principal.id")
    @Operation(summary = "Obtenir un utilisateur par son identifiant")
    public ResponseEntity<ApiResponse<UtilisateurDto>> trouverParId(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(utilisateurService.trouverParId(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','SUPER_ADMINISTRATEUR')")
    @Operation(summary = "Créer un nouvel utilisateur et envoyer l'invitation par email")
    public ResponseEntity<ApiResponse<UtilisateurDto>> creer(
            @Valid @RequestBody CreerUtilisateurRequest request) {
        UtilisateurDto created = utilisateurService.creer(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Utilisateur créé. Un email d'invitation a été envoyé.", created));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','SUPER_ADMINISTRATEUR') or #id == authentication.principal.id")
    @Operation(summary = "Modifier partiellement un utilisateur")
    public ResponseEntity<ApiResponse<UtilisateurDto>> modifier(
            @PathVariable Long id,
            @Valid @RequestBody ModifierUtilisateurRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Utilisateur modifié", utilisateurService.modifier(id, request)));
    }

    @PatchMapping("/{id}/desactiver")
    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','SUPER_ADMINISTRATEUR')")
    @Operation(summary = "Désactiver un compte utilisateur")
    public ResponseEntity<ApiResponse<Void>> desactiver(@PathVariable Long id) {
        utilisateurService.desactiver(id);
        return ResponseEntity.ok(ApiResponse.success("Compte désactivé", null));
    }

    @PatchMapping("/{id}/activer")
    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','SUPER_ADMINISTRATEUR')")
    @Operation(summary = "Réactiver un compte utilisateur")
    public ResponseEntity<ApiResponse<Void>> activer(@PathVariable Long id) {
        utilisateurService.activer(id);
        return ResponseEntity.ok(ApiResponse.success("Compte activé", null));
    }

    @PostMapping("/{id}/renvoyer-invitation")
    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','SUPER_ADMINISTRATEUR')")
    @Operation(summary = "Renvoyer l'email d'invitation à un utilisateur dont le compte n'est pas encore activé")
    public ResponseEntity<ApiResponse<Void>> renvoyerInvitation(@PathVariable Long id) {
        utilisateurService.renvoyerInvitation(id);
        return ResponseEntity.ok(ApiResponse.success("Invitation renvoyée par email.", null));
    }
}
