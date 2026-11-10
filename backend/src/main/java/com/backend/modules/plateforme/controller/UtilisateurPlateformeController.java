package com.backend.modules.plateforme.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.common.exception.BusinessRuleException;
import com.backend.modules.auth.security.UtilisateurPrincipal;
import com.backend.modules.plateforme.dto.CompteUtilisateurDto;
import com.backend.modules.plateforme.dto.ConfirmerImportUtilisateurRequest;
import com.backend.modules.plateforme.dto.CreerAdministrateurLaboratoireRequest;
import com.backend.modules.plateforme.dto.FichierGenere;
import com.backend.modules.plateforme.dto.ModifierIdentiteCompteRequest;
import com.backend.modules.plateforme.dto.MotifFacultatifRequest;
import com.backend.modules.plateforme.dto.PermissionsCompteDto;
import com.backend.modules.plateforme.dto.RemplacerEcartsRequest;
import com.backend.modules.plateforme.dto.RapportImportUtilisateurDto;
import com.backend.modules.plateforme.service.AdministrateurLaboratoireService;
import com.backend.modules.plateforme.service.PermissionCompteService;
import com.backend.modules.plateforme.service.UtilisateurImportService;
import com.backend.modules.plateforme.service.UtilisateurPlateformeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/plateforme/utilisateurs")
@PreAuthorize("hasRole('SUPER_ADMINISTRATEUR')")
@RequiredArgsConstructor
@Tag(name = "Utilisateurs plateforme", description = "Administrateurs de laboratoire, sans donnée métier de tenant")
public class UtilisateurPlateformeController {

    private final UtilisateurPlateformeService utilisateurPlateformeService;
    private final UtilisateurImportService utilisateurImportService;
    private final AdministrateurLaboratoireService administrateurLaboratoireService;
    private final PermissionCompteService permissionCompteService;

    @GetMapping
    @Operation(summary = "Lister les administrateurs de laboratoire")
    public ResponseEntity<ApiResponse<Page<CompteUtilisateurDto>>> lister(
            @RequestParam(required = false) String recherche,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Long laboratoireId,
            @RequestParam(required = false) String statut,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "nom") String tri,
            @RequestParam(defaultValue = "asc") String ordre) {
        return ResponseEntity.ok(ApiResponse.success(
                utilisateurPlateformeService.lister(recherche, role, laboratoireId, statut, page, size, tri, ordre)));
    }

    @GetMapping("/export")
    @Operation(summary = "Exporter les administrateurs de laboratoire (CSV, Excel ou PDF)")
    public ResponseEntity<byte[]> exporter(
            @RequestParam String format,
            @RequestParam(required = false) String recherche,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Long laboratoireId,
            @RequestParam(required = false) String statut,
            @RequestParam(defaultValue = "nom") String tri,
            @RequestParam(defaultValue = "asc") String ordre) {
        return fichier(utilisateurPlateformeService.exporter(
                format, recherche, role, laboratoireId, statut, tri, ordre));
    }

    @PostMapping
    @Operation(summary = "Créer un administrateur de laboratoire")
    public ResponseEntity<ApiResponse<CompteUtilisateurDto>> creer(
            @Valid @RequestBody CreerAdministrateurLaboratoireRequest request) {
        String acteur = acteur().getEmail();
        var cree = administrateurLaboratoireService.creer(request, acteur);
        return ResponseEntity.ok(ApiResponse.success("Administrateur créé", versCompte(cree)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un compte de connexion")
    public ResponseEntity<ApiResponse<CompteUtilisateurDto>> detail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(utilisateurPlateformeService.detail(id)));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Modifier l'identité d'un compte (e-mail, rôle et laboratoire en lecture seule)")
    public ResponseEntity<ApiResponse<CompteUtilisateurDto>> modifier(
            @PathVariable Long id,
            @Valid @RequestBody ModifierIdentiteCompteRequest request) {
        UtilisateurPrincipal acteur = acteur();
        return ResponseEntity.ok(ApiResponse.success(
                "Compte mis à jour",
                utilisateurPlateformeService.modifierIdentite(id, request, acteur.getId(), acteur.getEmail())));
    }

    @GetMapping("/{id}/permissions")
    @Operation(summary = "Permissions effectives d'un compte")
    public ResponseEntity<ApiResponse<PermissionsCompteDto>> permissions(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(permissionCompteService.lire(id)));
    }

    @PutMapping("/{id}/permissions")
    @Operation(summary = "Remplacer l'ensemble des écarts de permission")
    public ResponseEntity<ApiResponse<PermissionsCompteDto>> remplacerPermissions(
            @PathVariable Long id,
            @Valid @RequestBody RemplacerEcartsRequest request) {
        UtilisateurPrincipal acteur = acteur();
        return ResponseEntity.ok(ApiResponse.success(
                "Permissions mises à jour",
                permissionCompteService.remplacer(id, request, acteur.getId(), acteur.getEmail())));
    }

    @PostMapping("/{id}/permissions/retablir")
    @Operation(summary = "Rétablir les permissions par défaut du rôle")
    public ResponseEntity<ApiResponse<PermissionsCompteDto>> retablirPermissions(@PathVariable Long id) {
        UtilisateurPrincipal acteur = acteur();
        return ResponseEntity.ok(ApiResponse.success(
                "Permissions rétablies",
                permissionCompteService.retablir(id, acteur.getId(), acteur.getEmail())));
    }

    @PostMapping("/{id}/desactiver")
    @Operation(summary = "Désactiver un compte et révoquer ses sessions")
    public ResponseEntity<ApiResponse<CompteUtilisateurDto>> desactiver(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) MotifFacultatifRequest request) {
        UtilisateurPrincipal acteur = acteur();
        return ResponseEntity.ok(ApiResponse.success(
                "Compte désactivé",
                utilisateurPlateformeService.desactiver(id, acteur.getId(), acteur.getEmail(), motif(request))));
    }

    @PostMapping("/{id}/reactiver")
    @Operation(summary = "Réactiver un compte")
    public ResponseEntity<ApiResponse<CompteUtilisateurDto>> reactiver(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) MotifFacultatifRequest request) {
        UtilisateurPrincipal acteur = acteur();
        return ResponseEntity.ok(ApiResponse.success(
                "Compte réactivé",
                utilisateurPlateformeService.reactiver(id, acteur.getId(), acteur.getEmail(), motif(request))));
    }

    @PostMapping("/{id}/reinitialiser-2fa")
    @Operation(summary = "Réinitialiser la double authentification d'un administrateur")
    public ResponseEntity<ApiResponse<CompteUtilisateurDto>> reinitialiserDeuxFacteurs(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) MotifFacultatifRequest request) {
        UtilisateurPrincipal acteur = acteur();
        return ResponseEntity.ok(ApiResponse.success(
                "Double authentification réinitialisée",
                utilisateurPlateformeService.reinitialiserDeuxFacteurs(id, acteur.getId(), acteur.getEmail(), motif(request))));
    }

    @GetMapping("/{id}/sessions")
    @Operation(summary = "Sessions ouvertes d'un administrateur")
    public ResponseEntity<ApiResponse<java.util.List<com.backend.modules.plateforme.dto.SessionCompteDto>>> sessions(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(utilisateurPlateformeService.sessions(id)));
    }

    @PostMapping("/{id}/sessions/revoquer")
    @Operation(summary = "Déconnecter un administrateur de tous ses appareils")
    public ResponseEntity<ApiResponse<Integer>> revoquerSessions(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) MotifFacultatifRequest request) {
        UtilisateurPrincipal acteur = acteur();
        int sessions = utilisateurPlateformeService.revoquerSessions(id, acteur.getId(), acteur.getEmail(), motif(request));
        return ResponseEntity.ok(ApiResponse.success(sessions + " session(s) fermée(s)", sessions));
    }

    @PostMapping("/{id}/reinitialiser-mot-de-passe")
    @Operation(summary = "Réinitialiser le mot de passe d'un administrateur")
    public ResponseEntity<ApiResponse<?>> reinitialiserMotDePasse(@PathVariable Long id) {
        String acteur = acteur().getEmail();
        return ResponseEntity.ok(ApiResponse.success(
                "Mot de passe réinitialisé",
                administrateurLaboratoireService.reinitialiserMotDePasse(id, acteur)));
    }

    @PostMapping("/{id}/renvoyer-invitation")
    @Operation(summary = "Renvoyer l'invitation d'un administrateur")
    public ResponseEntity<ApiResponse<?>> renvoyerInvitation(@PathVariable Long id) {
        String acteur = acteur().getEmail();
        return ResponseEntity.ok(ApiResponse.success(
                "Invitation renvoyée",
                administrateurLaboratoireService.renvoyerInvitation(id, acteur)));
    }

    @GetMapping("/import/modele")
    @Operation(summary = "Télécharger le modèle d'importation")
    public ResponseEntity<byte[]> modele(@RequestParam String format) {
        return fichier(utilisateurImportService.modele(format));
    }

    @PostMapping(value = "/import/analyser", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Analyser un fichier d'importation sans rien écrire")
    public ResponseEntity<ApiResponse<RapportImportUtilisateurDto>> analyser(
            @RequestParam("fichier") MultipartFile fichier) {
        return ResponseEntity.ok(ApiResponse.success(utilisateurImportService.analyser(fichier, acteur().getEmail())));
    }

    @PostMapping("/import/confirmer")
    @Operation(summary = "Créer les lignes valides d'une importation")
    public ResponseEntity<ApiResponse<RapportImportUtilisateurDto>> confirmer(
            @Valid @RequestBody ConfirmerImportUtilisateurRequest request) {
        String acteur = acteur().getEmail();
        return ResponseEntity.ok(ApiResponse.success(
                "Importation terminée",
                utilisateurImportService.confirmer(request.getJetonAnalyse(), acteur)));
    }

    private UtilisateurPrincipal acteur() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UtilisateurPrincipal principal
                && principal.getEmail() != null && !principal.getEmail().isBlank()) {
            return principal;
        }
        throw new BusinessRuleException("ACTEUR_INTROUVABLE", "Utilisateur authentifié introuvable");
    }

    private static String motif(MotifFacultatifRequest request) {
        return request == null ? null : request.getMotif();
    }

    private static CompteUtilisateurDto versCompte(
            com.backend.modules.plateforme.dto.CompteAdministrateurDto admin) {
        return CompteUtilisateurDto.builder()
                .id(admin.getId())
                .nom(admin.getNom())
                .prenom(admin.getPrenom())
                .email(admin.getEmail())
                .telephone(admin.getTelephone())
                .fonction(admin.getFonction())
                .role("ADMINISTRATEUR")
                .laboratoireId(admin.getLaboratoireId())
                .laboratoireNom(admin.getLaboratoireNom())
                .statut("ACTIF".equals(admin.getStatut()) ? "ACTIF" : "INACTIF")
                .derniereConnexion(admin.getDerniereConnexion())
                .build();
    }

    private static ResponseEntity<byte[]> fichier(FichierGenere genere) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + genere.nom() + "\"")
                .contentType(MediaType.parseMediaType(genere.typeMime()))
                .body(genere.contenu());
    }
}
