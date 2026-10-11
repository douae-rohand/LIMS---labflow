package com.backend.modules.plateforme.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.common.exception.BusinessRuleException;
import com.backend.integration.sendgrid.EmailService;
import com.backend.integration.sendgrid.ResultatEnvoiEmail;
import com.backend.integration.sendgrid.SendGridProperties;
import com.backend.modules.plateforme.dto.*;
import com.backend.modules.plateforme.entity.StatutIntegration;
import com.backend.modules.plateforme.entity.TypeDocumentIntegration;
import com.backend.modules.plateforme.service.DemandeIntegrationService;
import com.backend.modules.plateforme.service.LaboratoireExportService;
import com.backend.modules.plateforme.service.LaboratoireImportService;
import com.backend.modules.plateforme.service.PlateformeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Endpoints de gestion de la plateforme (M15).
 * Réservés au SUPER_ADMINISTRATEUR sauf la soumission de demande.
 * Base path : {@code /api/plateforme}
 */
@RestController
@RequestMapping("/api/plateforme")
@RequiredArgsConstructor
@Tag(name = "Plateforme", description = "Gestion des laboratoires et demandes d'intégration (M15)")
public class PlateformeController {

    private final PlateformeService plateformeService;
    private final DemandeIntegrationService demandeIntegrationService;
    private final LaboratoireExportService laboratoireExportService;
    private final LaboratoireImportService laboratoireImportService;
    private final EmailService emailService;
    private final SendGridProperties sendGridProperties;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @org.springframework.beans.factory.annotation.Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    // -------------------------------------------------------------------------
    // Laboratoires
    // -------------------------------------------------------------------------

    @GetMapping("/laboratoires")
    @PreAuthorize("hasRole('SUPER_ADMINISTRATEUR')")
    @Operation(summary = "Rechercher les laboratoires")
    public ResponseEntity<ApiResponse<Page<LaboratoireDto>>> listerLaboratoires(
            @RequestParam(required = false) String recherche,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String ville,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "dateCreation") String tri,
            @RequestParam(defaultValue = "desc") String ordre) {
        return ResponseEntity.ok(ApiResponse.success(
                plateformeService.rechercherLaboratoires(recherche, statut, ville, page, size, tri, ordre)));
    }

    @GetMapping("/laboratoires/statistiques")
    @PreAuthorize("hasRole('SUPER_ADMINISTRATEUR')")
    @Operation(summary = "Indicateurs de la plateforme")
    public ResponseEntity<ApiResponse<StatistiquesPlateformeDto>> statistiques() {
        return ResponseEntity.ok(ApiResponse.success(plateformeService.statistiques()));
    }

    @GetMapping("/laboratoires/villes")
    @PreAuthorize("hasRole('SUPER_ADMINISTRATEUR')")
    @Operation(summary = "Lister les villes des laboratoires")
    public ResponseEntity<ApiResponse<java.util.List<String>>> listerVilles() {
        return ResponseEntity.ok(ApiResponse.success(plateformeService.listerVilles()));
    }

    @GetMapping("/laboratoires/export")
    @PreAuthorize("hasRole('SUPER_ADMINISTRATEUR')")
    @Operation(summary = "Exporter les laboratoires en PDF, Excel ou CSV")
    public ResponseEntity<byte[]> exporterLaboratoires(
            @RequestParam String format,
            @RequestParam(defaultValue = "filtres") String portee,
            @RequestParam(required = false) String recherche,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String ville) {
        boolean tous = "tous".equalsIgnoreCase(portee);
        FichierGenere fichier = laboratoireExportService.exporter(format, recherche, statut, ville, tous);
        return fichier(fichier);
    }

    @GetMapping("/laboratoires/import/modele")
    @PreAuthorize("hasRole('SUPER_ADMINISTRATEUR')")
    @Operation(summary = "Télécharger le modèle d'importation Excel ou CSV")
    public ResponseEntity<byte[]> modeleImport(@RequestParam String format) {
        return fichier(laboratoireImportService.modele(format));
    }

    @PostMapping(value = "/laboratoires/import/analyser", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SUPER_ADMINISTRATEUR')")
    @Operation(summary = "Analyser un fichier d'importation sans rien enregistrer")
    public ResponseEntity<ApiResponse<RapportImportLaboratoireDto>> analyserImport(
            @RequestPart("fichier") MultipartFile fichier) {
        return ResponseEntity.ok(ApiResponse.success(laboratoireImportService.analyser(fichier)));
    }

    @PostMapping("/laboratoires/import/verifier")
    @PreAuthorize("hasRole('SUPER_ADMINISTRATEUR')")
    @Operation(summary = "Revérifier des lignes corrigées sans les enregistrer")
    public ResponseEntity<ApiResponse<RapportImportLaboratoireDto>> verifierImport(
            @Valid @RequestBody ConfirmerImportLaboratoireRequest request) {
        return ResponseEntity.ok(ApiResponse.success(laboratoireImportService.verifier(request.getLignes())));
    }

    @PostMapping("/laboratoires/import/confirmer")
    @PreAuthorize("hasRole('SUPER_ADMINISTRATEUR')")
    @Operation(summary = "Créer les demandes d'intégration pour les lignes valides")
    public ResponseEntity<ApiResponse<RapportImportLaboratoireDto>> confirmerImport(
            @Valid @RequestBody ConfirmerImportLaboratoireRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Importation terminée", laboratoireImportService.confirmer(request.getLignes())));
    }

    @GetMapping("/laboratoires/{id}/demande")
    @PreAuthorize("hasRole('SUPER_ADMINISTRATEUR')")
    @Operation(summary = "Demande d'intégration rattachée à un laboratoire, avec ses documents")
    public ResponseEntity<ApiResponse<DemandeIntegrationDto>> demandeDuLaboratoire(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(demandeIntegrationService.trouverParLaboratoire(id)));
    }

    @GetMapping("/laboratoires/{code}")
    @PreAuthorize("hasRole('SUPER_ADMINISTRATEUR')")
    @Operation(summary = "Obtenir un laboratoire par son code")
    public ResponseEntity<ApiResponse<LaboratoireDto>> trouverLaboratoire(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.success(plateformeService.trouverParCode(code)));
    }

    @PostMapping("/laboratoires")
    @PreAuthorize("hasRole('SUPER_ADMINISTRATEUR')")
    @Operation(summary = "Créer un nouveau laboratoire (tenant)")
    public ResponseEntity<ApiResponse<LaboratoireDto>> creerLaboratoire(
            @Valid @RequestBody CreerLaboratoireRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Laboratoire créé", plateformeService.creerLaboratoire(request)));
    }

    @PatchMapping("/laboratoires/{id}")
    @PreAuthorize("hasRole('SUPER_ADMINISTRATEUR')")
    @Operation(summary = "Modifier les informations d'un laboratoire")
    public ResponseEntity<ApiResponse<LaboratoireDto>> modifierLaboratoire(
            @PathVariable Long id,
            @Valid @RequestBody ModifierLaboratoireRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Laboratoire mis à jour", plateformeService.modifierLaboratoire(id, request)));
    }

    @PatchMapping("/laboratoires/{id}/administrateur")
    @PreAuthorize("hasRole('SUPER_ADMINISTRATEUR')")
    @Operation(summary = "Modifier l'administrateur d'un laboratoire")
    public ResponseEntity<ApiResponse<LaboratoireDto>> modifierAdministrateur(
            @PathVariable Long id,
            @Valid @RequestBody ModifierAdministrateurLaboratoireRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Administrateur mis à jour", plateformeService.modifierAdministrateur(id, request)));
    }

    @PatchMapping("/laboratoires/{id}/activer")
    @PreAuthorize("hasRole('SUPER_ADMINISTRATEUR')")
    @Operation(summary = "Activer un laboratoire")
    public ResponseEntity<ApiResponse<LaboratoireDto>> activerLaboratoire(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Laboratoire activé", plateformeService.activerLaboratoire(id)));
    }

    @PatchMapping("/laboratoires/{id}/desactiver")
    @PreAuthorize("hasRole('SUPER_ADMINISTRATEUR')")
    @Operation(summary = "Désactiver un laboratoire sans supprimer ses données")
    public ResponseEntity<ApiResponse<LaboratoireDto>> desactiverLaboratoire(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Laboratoire désactivé", plateformeService.desactiverLaboratoire(id)));
    }

    // -------------------------------------------------------------------------
    // Demandes d'intégration
    // -------------------------------------------------------------------------

    @PostMapping(value = "/demandes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Soumettre une demande d'intégration (public, multipart)")
    public ResponseEntity<ApiResponse<DemandeIntegrationDto>> soumettreDemande(
            @RequestPart("demande") MultipartFile demandeJson,
            @RequestPart("AUTORISATION_OUVERTURE_EXPLOITATION") MultipartFile autorisation,
            @RequestPart("DIPLOME_BIOLOGISTE_RESPONSABLE") MultipartFile diplome,
            @RequestPart("INSCRIPTION_ORDRE_PROFESSIONNEL") MultipartFile inscription,
            @RequestPart("CIN_BIOLOGISTE_RESPONSABLE") MultipartFile cin,
            @RequestPart("IDENTIFICATION_JURIDIQUE") MultipartFile identification,
            @RequestPart("JUSTIFICATIF_ADRESSE") MultipartFile justificatif) {
        SoumettreDemandeIntegrationRequest demande = lireDemande(demandeJson);
        Map<TypeDocumentIntegration, MultipartFile> fichiers = new EnumMap<>(TypeDocumentIntegration.class);
        fichiers.put(TypeDocumentIntegration.AUTORISATION_OUVERTURE_EXPLOITATION, autorisation);
        fichiers.put(TypeDocumentIntegration.DIPLOME_BIOLOGISTE_RESPONSABLE, diplome);
        fichiers.put(TypeDocumentIntegration.INSCRIPTION_ORDRE_PROFESSIONNEL, inscription);
        fichiers.put(TypeDocumentIntegration.CIN_BIOLOGISTE_RESPONSABLE, cin);
        fichiers.put(TypeDocumentIntegration.IDENTIFICATION_JURIDIQUE, identification);
        fichiers.put(TypeDocumentIntegration.JUSTIFICATIF_ADRESSE, justificatif);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Demande soumise",
                        demandeIntegrationService.soumettre(demande, fichiers)));
    }

    @GetMapping("/demandes")
    @PreAuthorize("hasRole('SUPER_ADMINISTRATEUR')")
    @Operation(summary = "Lister les demandes d'intégration")
    public ResponseEntity<ApiResponse<Page<DemandeIntegrationDto>>> listerDemandes(
            @RequestParam(required = false) StatutIntegration statut,
            @RequestParam(required = false) String recherche,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "dateDemande") String tri,
            @RequestParam(defaultValue = "desc") String ordre) {
        return ResponseEntity.ok(ApiResponse.success(
                demandeIntegrationService.lister(statut, recherche, page, size, tri, ordre)));
    }

    @GetMapping("/demandes/{id}")
    @PreAuthorize("hasRole('SUPER_ADMINISTRATEUR')")
    @Operation(summary = "Détail d'une demande d'intégration")
    public ResponseEntity<ApiResponse<DemandeIntegrationDto>> trouverDemande(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(demandeIntegrationService.trouver(id)));
    }

    @PostMapping("/demandes/{id}/traiter")
    @PreAuthorize("hasRole('SUPER_ADMINISTRATEUR')")
    @Operation(summary = "Approuver ou rejeter une demande d'intégration")
    public ResponseEntity<ApiResponse<DemandeIntegrationDto>> traiterDemande(
            @PathVariable Long id,
            @Valid @RequestBody TraiterDemandeIntegrationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Demande traitée",
                demandeIntegrationService.traiter(id, request)));
    }

    @PatchMapping("/demandes/{id}/traiter")
    @PreAuthorize("hasRole('SUPER_ADMINISTRATEUR')")
    @Operation(summary = "Approuver ou rejeter une demande (compatibilité query params)")
    public ResponseEntity<ApiResponse<DemandeIntegrationDto>> traiterDemandeLegacy(
            @PathVariable Long id,
            @RequestParam StatutIntegration decision,
            @RequestParam(required = false) String commentaire) {
        TraiterDemandeIntegrationRequest request = new TraiterDemandeIntegrationRequest();
        request.setDecision(decision);
        request.setMotifRefus(commentaire);
        return ResponseEntity.ok(ApiResponse.success("Demande traitée",
                demandeIntegrationService.traiter(id, request)));
    }

    @GetMapping("/demandes/{id}/documents/{type}")
    @PreAuthorize("hasRole('SUPER_ADMINISTRATEUR')")
    @Operation(summary = "Consulter ou télécharger un document d'intégration")
    public ResponseEntity<Resource> telechargerDocument(
            @PathVariable Long id,
            @PathVariable TypeDocumentIntegration type,
            @RequestParam(defaultValue = "false") boolean download) {
        return demandeIntegrationService.telechargerDocument(id, type, download);
    }

    @PostMapping(value = "/demandes/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SUPER_ADMINISTRATEUR')")
    @Operation(summary = "Ajouter ou remplacer un document justificatif")
    public ResponseEntity<ApiResponse<DemandeIntegrationDto>> ajouterDocument(
            @PathVariable Long id,
            @RequestParam TypeDocumentIntegration type,
            @RequestPart("fichier") MultipartFile fichier) {
        return ResponseEntity.ok(ApiResponse.success(
                "Document enregistré", demandeIntegrationService.ajouterDocument(id, type, fichier)));
    }

    @PostMapping("/demandes/{id}/invitation")
    @PreAuthorize("hasRole('SUPER_ADMINISTRATEUR')")
    @Operation(summary = "Renvoyer l'e-mail d'activation à l'administrateur laboratoire")
    public ResponseEntity<ApiResponse<ResultatEnvoiEmail>> renvoyerInvitation(@PathVariable Long id) {
        ResultatEnvoiEmail resultat = demandeIntegrationService.renvoyerInvitation(id);
        String message = resultat.accepte()
                ? "Invitation renvoyée et acceptée par SendGrid"
                : "Jeton régénéré mais e-mail non envoyé : " + resultat.message();
        return ResponseEntity.ok(ApiResponse.success(message, resultat));
    }

    @GetMapping("/emails/statut")
    @PreAuthorize("hasRole('SUPER_ADMINISTRATEUR')")
    @Operation(summary = "État de la configuration SendGrid (sans exposer la clé)")
    public ResponseEntity<ApiResponse<StatutSendGridDto>> statutSendGrid() {
        return ResponseEntity.ok(ApiResponse.success(new StatutSendGridDto(
                sendGridProperties.isEnabled(),
                sendGridProperties.pretPourEnvoi(),
                sendGridProperties.getFromEmail(),
                sendGridProperties.getFromName(),
                frontendUrl)));
    }

    @PostMapping("/emails/test")
    @PreAuthorize("hasRole('SUPER_ADMINISTRATEUR')")
    @Operation(summary = "Envoyer un e-mail de test SendGrid")
    public ResponseEntity<ApiResponse<ResultatEnvoiEmail>> testerEmail(
            @Valid @RequestBody TesterEmailRequest request) {
        ResultatEnvoiEmail resultat = emailService.envoyerTest(request.getDestinataire());
        return ResponseEntity.ok(ApiResponse.success(
                resultat.accepte() ? "E-mail de test accepté par SendGrid" : resultat.message(),
                resultat));
    }

    private SoumettreDemandeIntegrationRequest lireDemande(MultipartFile demandeJson) {
        try {
            SoumettreDemandeIntegrationRequest demande = objectMapper.readValue(
                    demandeJson.getBytes(), SoumettreDemandeIntegrationRequest.class);
            Set<ConstraintViolation<SoumettreDemandeIntegrationRequest>> violations = validator.validate(demande);
            if (!violations.isEmpty()) {
                String details = violations.stream()
                        .map(v -> v.getPropertyPath() + " : " + v.getMessage())
                        .collect(Collectors.joining("; "));
                throw new BusinessRuleException("DEMANDE_INVALIDE", details);
            }
            return demande;
        } catch (IOException ex) {
            throw new BusinessRuleException("DEMANDE_INVALIDE",
                    "Le contenu JSON de la demande est illisible");
        }
    }

    private ResponseEntity<byte[]> fichier(FichierGenere fichier) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fichier.nom() + "\"")
                .contentType(MediaType.parseMediaType(fichier.typeMime()))
                .body(fichier.contenu());
    }
}
