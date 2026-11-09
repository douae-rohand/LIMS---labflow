package com.backend.modules.technicien.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.common.dto.PageResponse;
import com.backend.modules.essai.entity.StatutEssai;
import com.backend.modules.technicien.dto.*;
import com.backend.modules.technicien.service.TechnicienImportService;
import com.backend.modules.technicien.service.TechnicienService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Espace Technicien — /api/technicien/**
 *
 * Toutes les routes filtrent par technicienId = principal courant.
 * Même un Responsable ou Administrateur possédant les permissions
 * ne voit que ses propres essais sur ces routes.
 *
 * Sécurité :
 *   - hasAuthority('PERM_PLANIFICATION_STATUT') pour la lecture
 *   - hasAuthority('PERM_ESSAIS_GERER') pour la saisie
 *   - hasAuthority('PERM_VALIDATION_TECHNIQUE_GERER') pour la validation N1
 *   - hasAuthority('PERM_ESSAIS_IMPORT_CSV') pour l'import
 *
 * Les permissions sont rechargées depuis la BD à chaque requête JWT ;
 * un retrait d'autorisation prend effet immédiatement.
 */
@RestController
@RequestMapping("/api/technicien")
@RequiredArgsConstructor
@Tag(name = "Espace Technicien", description = "Gestion des essais assignés au technicien courant")
public class TechnicienController {

    private final TechnicienService technicienService;
    private final TechnicienImportService importService;

    // =========================================================================
    // LOT A — Lecture
    // =========================================================================

    /**
     * GET /api/technicien/essais
     * Liste paginée des essais du technicien courant.
     *
     * Paramètres de pagination :
     *   page (défaut 0), size (défaut 20, max 50), tri (défaut dateAttribution), direction (asc/desc)
     *
     * Filtres :
     *   statut, domaineId, demandeId, debut, fin (ISO-8601 instant), enRetard, q
     */
    @GetMapping("/essais")
    @PreAuthorize("hasAuthority('PERM_PLANIFICATION_STATUT')")
    @Operation(summary = "Lister mes essais (paginé)")
    public ApiResponse<PageResponse<LigneEssaiTechnicienDto>> listerMesEssais(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "dateAttribution") String tri,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) StatutEssai statut,
            @RequestParam(required = false) Long domaineId,
            @RequestParam(required = false) Long demandeId,
            @RequestParam(required = false) Instant debut,
            @RequestParam(required = false) Instant fin,
            @RequestParam(defaultValue = "false") boolean enRetard,
            @RequestParam(defaultValue = "") String q) {

        return ApiResponse.success(
                technicienService.listerMesEssais(
                        page, size, tri, direction,
                        statut, domaineId, demandeId,
                        debut, fin, enRetard, q));
    }

    /**
     * GET /api/technicien/essais/{id}
     * Détail d'un essai avec exigences, historique de validations et temps déclaré.
     * Retourne 404 si l'essai n'existe pas OU n'appartient pas au technicien courant.
     */
    @GetMapping("/essais/{id}")
    @PreAuthorize("hasAuthority('PERM_PLANIFICATION_STATUT')")
    @Operation(summary = "Détail d'un essai")
    public ApiResponse<LigneEssaiDetailTechnicienDto> detailEssai(@PathVariable Long id) {
        return ApiResponse.success(technicienService.detailEssai(id));
    }

    /**
     * GET /api/technicien/planning?debut=&fin=
     * Essais regroupés par jour (vue calendrier).
     * Plafond : 62 jours. Retourne 422 si dépassé.
     */
    @GetMapping("/planning")
    @PreAuthorize("hasAuthority('PERM_PLANIFICATION_STATUT')")
    @Operation(summary = "Planning du technicien (vue calendrier)")
    public ApiResponse<List<PlanningJourDto>> planning(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return ApiResponse.success(technicienService.planning(debut, fin));
    }

    /**
     * GET /api/technicien/synthese
     * Tableau de bord : compteurs agrégés, charge de la semaine.
     */
    @GetMapping("/synthese")
    @PreAuthorize("hasAuthority('PERM_PLANIFICATION_STATUT')")
    @Operation(summary = "Tableau de bord du technicien")
    public ApiResponse<SyntheseTechnicienDto> synthese() {
        return ApiResponse.success(technicienService.synthese());
    }

    // =========================================================================
    // LOT B — Saisie
    // =========================================================================

    /**
     * POST /api/technicien/essais/{id}/demarrer
     * Passe la ligne EN_COURS. Vérifie présence et conformité de l'échantillon.
     * Transition demande ACCEPTEE → EN_COURS si premier démarrage.
     */
    @PostMapping("/essais/{id}/demarrer")
    @PreAuthorize("hasAuthority('PERM_ESSAIS_GERER')")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Démarrer un essai")
    public ApiResponse<LigneEssaiTechnicienDto> demarrer(@PathVariable Long id) {
        return ApiResponse.success("Essai démarré.", technicienService.demarrer(id));
    }

    /**
     * PUT /api/technicien/essais/{id}/resultat
     * Saisit ou modifie le résultat. Classification CONFORME/NON_CONFORME/CRITIQUE calculée serveur.
     * Motif obligatoire si résultat déjà saisi. Interdit si validation Responsable présente.
     */
    @PutMapping("/essais/{id}/resultat")
    @PreAuthorize("hasAuthority('PERM_ESSAIS_GERER')")
    @Operation(summary = "Saisir ou modifier le résultat d'un essai")
    public ApiResponse<LigneEssaiTechnicienDto> saisirResultat(
            @PathVariable Long id,
            @RequestBody @Valid ResultatSaisieRequest req) {
        return ApiResponse.success("Résultat enregistré.", technicienService.saisirResultat(id, req));
    }

    /**
     * POST /api/technicien/essais/{id}/non-realisable
     * Déclare l'essai non réalisable avec un motif obligatoire.
     */
    @PostMapping("/essais/{id}/non-realisable")
    @PreAuthorize("hasAuthority('PERM_ESSAIS_GERER')")
    @Operation(summary = "Déclarer un essai non réalisable")
    public ApiResponse<LigneEssaiTechnicienDto> declareNonRealisable(
            @PathVariable Long id,
            @RequestBody @Valid NonRealisableRequest req) {
        return ApiResponse.success("Essai déclaré non réalisable.", technicienService.declareNonRealisable(id, req));
    }

    /**
     * POST /api/technicien/essais/{id}/temps
     * Déclare le temps passé (début/fin ou durée en minutes).
     * Une seule valeur stockée — la dernière remplace la précédente.
     */
    @PostMapping("/essais/{id}/temps")
    @PreAuthorize("hasAuthority('PERM_ESSAIS_GERER')")
    @Operation(summary = "Déclarer le temps passé sur un essai")
    public ApiResponse<LigneEssaiTechnicienDto> declarerTemps(
            @PathVariable Long id,
            @RequestBody @Valid TempsRequest req) {
        return ApiResponse.success("Temps enregistré.", technicienService.declarerTemps(id, req));
    }

    /**
     * POST /api/technicien/essais/{id}/validation-technique
     * Valide techniquement (niveau 1). La décision est toujours VALIDEE.
     * Historique conservé (ISO 17025). Transition demande → EN_VALIDATION si toutes les lignes closes.
     */
    @PostMapping("/essais/{id}/validation-technique")
    @PreAuthorize("hasAuthority('PERM_VALIDATION_TECHNIQUE_GERER')")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Soumettre la validation technique (niveau 1)")
    public ApiResponse<LigneEssaiTechnicienDto> validerTechniquement(
            @PathVariable Long id,
            @RequestBody @Valid ValidationTechniqueRequest req) {
        return ApiResponse.success("Validation technique enregistrée.", technicienService.validerTechniquement(id, req));
    }

    // =========================================================================
    // LOT C — Import CSV
    // =========================================================================

    /**
     * POST /api/technicien/essais/import/apercu
     * Parse et valide le CSV sans écriture.
     * Retourne l'aperçu des lignes valides et la liste complète des erreurs.
     *
     * Format CSV : colonnes {@code code_ligne;valeur;commentaire}
     * Séparateur ';' ou ',' détecté automatiquement. Encodage UTF-8.
     */
    @PostMapping(value = "/essais/import/apercu", consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('PERM_ESSAIS_IMPORT_CSV')")
    @Operation(summary = "Aperçu d'import CSV (sans écriture)")
    public ApiResponse<ImportApercuResponse> importApercu(
            @RequestParam("fichier") MultipartFile fichier) {
        return ApiResponse.success(importService.apercu(fichier));
    }

    /**
     * POST /api/technicien/essais/import/confirmer
     * Applique l'import en une transaction : tout ou rien.
     * Une ligne invalide → rollback complet + 422 avec liste des erreurs.
     */
    @PostMapping(value = "/essais/import/confirmer", consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('PERM_ESSAIS_IMPORT_CSV')")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Confirmer l'import CSV (écriture transactionnelle)")
    public ApiResponse<ImportConfirmerResponse> importConfirmer(
            @RequestParam("fichier") MultipartFile fichier) {
        try {
            return ApiResponse.success("Import réussi.", importService.confirmer(fichier));
        } catch (TechnicienImportService.ImportValidationException ex) {
            // Erreurs de validation structurées → 422 avec détail par ligne
            return ApiResponse.<ImportConfirmerResponse>builder()
                    .success(false)
                    .message(ex.getMessage())
                    .data(ImportConfirmerResponse.builder()
                            .lignesImportees(0)
                            .erreurs(ex.getErreurs())
                            .build())
                    .build();
        }
    }
}
