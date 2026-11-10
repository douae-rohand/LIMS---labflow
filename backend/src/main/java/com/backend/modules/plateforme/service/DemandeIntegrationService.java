package com.backend.modules.plateforme.service;

import com.backend.common.audit.JournalAuditService;
import com.backend.common.exception.BusinessRuleException;
import com.backend.common.exception.ResourceNotFoundException;
import com.backend.common.tenant.TenantProvisioner;
import com.backend.common.util.ApresCommit;
import com.backend.common.util.ExportTableau;
import com.backend.integration.minio.StockageFichierService;
import com.backend.integration.n8n.NotificationEventPublisher;
import com.backend.integration.sendgrid.EmailService;
import com.backend.integration.sendgrid.ResultatEnvoiEmail;
import com.backend.modules.auth.service.ActivationCompteService;
import com.backend.modules.notification.service.NotificationService;
import com.backend.modules.plateforme.dto.*;
import com.backend.modules.plateforme.entity.*;
import com.backend.modules.plateforme.repository.DemandeIntegrationRepository;
import com.backend.modules.plateforme.repository.DocumentIntegrationRepository;
import com.backend.modules.plateforme.repository.LaboratoireRepository;
import com.backend.modules.utilisateur.entity.Role;
import com.backend.modules.utilisateur.entity.RoleUtilisateur;
import com.backend.modules.utilisateur.entity.Utilisateur;
import com.backend.modules.utilisateur.repository.RoleRepository;
import com.backend.modules.utilisateur.repository.UtilisateurRepository;
import com.backend.modules.utilisateur.service.MatriculeGeneratorService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DemandeIntegrationService {

    private static final String STATUT_ACTIF = "ACTIF";
    private static final Set<String> MIME_AUTORISES = Set.of(
            "application/pdf",
            "application/x-pdf",
            "image/jpeg",
            "image/png",
            "image/webp"
    );
    private static final Set<String> EXTENSIONS_AUTORISEES = Set.of("pdf", "jpg", "jpeg", "png", "webp");
    private static final Set<StatutIntegration> STATUTS_OUVERTS =
            EnumSet.of(StatutIntegration.EN_ATTENTE, StatutIntegration.COMPLEMENTS_DEMANDES);
    private static final Duration VALIDITE_JETON_COMPLEMENTS = Duration.ofDays(14);
    private static final int EXPORT_MAX = 5000;
    private static final List<String> COLONNES_EXPORT = List.of(
            "Numéro", "Date de soumission", "Laboratoire", "Raison sociale", "Ville", "Types",
            "Contact", "E-mail du contact", "Statut", "Date de traitement", "Documents");
    private static final DateTimeFormatter DATE_EXPORT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("Africa/Casablanca"));
    private static final SecureRandom ALEA = new SecureRandom();

    private final DemandeIntegrationRepository demandeIntegrationRepository;
    private final DocumentIntegrationRepository documentIntegrationRepository;
    private final LaboratoireRepository laboratoireRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;
    private final TenantProvisioner tenantProvisioner;
    private final StockageFichierService stockageFichierService;
    private final ActivationCompteService activationCompteService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final MatriculeGeneratorService matriculeGeneratorService;
    private final JournalAuditService journalAuditService;
    private final NotificationEventPublisher notificationEventPublisher;
    private final ParametrePlateformeService parametrePlateformeService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @Value("${app.integration.max-file-size-bytes:10485760}")
    private long tailleMaxOctets;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Transactional(readOnly = true)
    public List<TypeDocumentIntegrationDto> listerDocumentsRequis() {
        return parametrePlateformeService.typesDocumentsActifs().stream()
                .map(TypeDocumentIntegrationDto::from)
                .toList();
    }

    @Transactional
    public DemandeIntegrationDto soumettre(SoumettreDemandeIntegrationRequest request,
                                           Map<String, MultipartFile> fichiers) {
        validerUnicite(request);
        List<TypeDocumentIntegration> actifs = parametrePlateformeService.typesDocumentsActifs();
        Map<String, MultipartFile> retenus = validerDocuments(actifs, fichiers == null ? Map.of() : fichiers);

        DemandeIntegration demande = DemandeIntegration.builder()
                .numero("INT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .dateDemande(Instant.now())
                .statut(StatutIntegration.EN_ATTENTE.name())
                .nomLaboratoire(request.getNomLaboratoire().trim())
                .raisonSociale(request.getRaisonSociale().trim())
                .typeLaboratoire(joindreTypes(request.getTypesLaboratoire()))
                .ice(normaliser(request.getIce()))
                .telephoneLaboratoire(normaliser(request.getTelephoneLaboratoire()))
                .emailLaboratoire(normaliser(request.getEmailLaboratoire()))
                .siteWeb(normaliser(request.getSiteWeb()))
                .message(normaliser(request.getInformationsComplementaires()))
                .adresse(request.getAdresse().trim())
                .ville(request.getVille().trim())
                .region(normaliser(request.getRegion()))
                .pays(StringUtils.hasText(request.getPays()) ? request.getPays().trim() : "Maroc")
                .codePostal(normaliser(request.getCodePostal()))
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .contactNom(request.getAdminNom().trim())
                .contactPrenom(request.getAdminPrenom().trim())
                .contactEmail(request.getAdminEmail().trim().toLowerCase())
                .contactTelephone(request.getAdminTelephone().trim())
                .contactFonction(request.getAdminFonction().trim())
                .contactCin(request.getAdminCin().trim())
                .build();

        demande = demandeIntegrationRepository.save(demande);

        Map<String, TypeDocumentIntegration> types = parametrePlateformeService.typesDocumentsParCode();
        for (Map.Entry<String, MultipartFile> entree : retenus.entrySet()) {
            enregistrerDocument(demande, types.get(entree.getKey()), entree.getValue());
        }

        DemandeIntegration confirmee = demande;
        int nombreDocuments = retenus.size();
        ApresCommit.executer(() -> {
            emailService.envoyerConfirmationDemandeIntegration(
                    emailsNotification(confirmee),
                    nomComplet(confirmee),
                    premierNonVide(confirmee.getNomLaboratoire(), confirmee.getRaisonSociale()),
                    confirmee.getNumero(),
                    confirmee.getVille(),
                    nombreDocuments,
                    confirmee.getDateDemande());
            notificationService.notifierRole(RoleUtilisateur.SUPER_ADMINISTRATEUR, "DEMANDE_INTEGRATION",
                    "Nouvelle demande d'intégration",
                    premierNonVide(confirmee.getNomLaboratoire(), confirmee.getRaisonSociale())
                            + " (" + confirmee.getVille() + ") — " + confirmee.getNumero(),
                    lienDemande(confirmee.getId()));
        });
        log.info("Demande d'intégration soumise : id={}, numero={}, labo={}",
                demande.getId(), demande.getNumero(), demande.getNomLaboratoire());
        return toDto(demande, true);
    }

    @Transactional(readOnly = true)
    public Page<DemandeIntegrationDto> lister(StatutIntegration statut, String recherche,
                                              int page, int size, String tri, String ordre) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                triDemandes(tri, ordre));
        return demandeIntegrationRepository
                .rechercher(statut == null ? null : statut.name(), terme(recherche), pageable)
                .map(d -> toDto(d, false));
    }

    @Transactional(readOnly = true)
    public FichierGenere exporter(String format, StatutIntegration statut, String recherche) {
        Page<DemandeIntegration> page = demandeIntegrationRepository.rechercher(
                statut == null ? null : statut.name(), terme(recherche),
                PageRequest.of(0, EXPORT_MAX, Sort.by(Sort.Direction.DESC, "dateDemande")));
        if (page.getTotalElements() > EXPORT_MAX) {
            throw new BusinessRuleException("EXPORT_TROP_LARGE",
                    "Plus de " + EXPORT_MAX + " lignes correspondent. Affinez les filtres avant d'exporter.");
        }
        List<List<String>> lignes = page.getContent().stream()
                .map(d -> Arrays.asList(
                        d.getNumero(),
                        d.getDateDemande() == null ? "" : DATE_EXPORT.format(d.getDateDemande()),
                        premierNonVide(d.getNomLaboratoire(), d.getRaisonSociale()),
                        d.getRaisonSociale(),
                        d.getVille(),
                        d.getTypeLaboratoire(),
                        nomComplet(d),
                        d.getContactEmail(),
                        libelleStatut(StatutIntegration.depuis(d.getStatut())),
                        d.getDateTraitement() == null ? "" : DATE_EXPORT.format(d.getDateTraitement()),
                        Long.toString(documentIntegrationRepository.countByDemande_Id(d.getId()))))
                .toList();
        return ExportTableau.generer(format, "demandes-integration", "Demandes d'intégration",
                COLONNES_EXPORT, lignes);
    }

    @Transactional(readOnly = true)
    public DemandeIntegrationDto trouver(Long id) {
        return toDto(charger(id), true);
    }

    @Transactional(readOnly = true)
    public DemandeIntegrationDto trouverParLaboratoire(Long laboratoireId) {
        return demandeIntegrationRepository.findFirstByLaboratoire_IdOrderByIdDesc(laboratoireId)
                .map(demande -> toDto(demande, true))
                .orElse(null);
    }

    @Transactional
    public DemandeIntegrationDto traiter(Long id, TraiterDemandeIntegrationRequest request, String acteurEmail) {
        DemandeIntegration demande = charger(id);
        StatutIntegration actuel = StatutIntegration.depuis(demande.getStatut());

        if (!STATUTS_OUVERTS.contains(actuel)) {
            throw new BusinessRuleException("DEMANDE_DEJA_TRAITEE",
                    "Cette demande a déjà été traitée (" + actuel + ")");
        }
        if (request.getDecision() != StatutIntegration.ACCEPTEE
                && request.getDecision() != StatutIntegration.REFUSEE) {
            throw new BusinessRuleException("DECISION_INVALIDE",
                    "La décision doit être ACCEPTEE ou REFUSEE");
        }

        String ancienStatut = demande.getStatut();
        String nouveauStatut = request.getDecision().name();

        if (request.getDecision() == StatutIntegration.REFUSEE) {
            if (!StringUtils.hasText(request.getMotifRefus())) {
                throw new BusinessRuleException("MOTIF_REFUS_OBLIGATOIRE",
                        "Le motif du refus est obligatoire");
            }
            demande.setStatut(StatutIntegration.REFUSEE.name());
            demande.setMotifRefus(request.getMotifRefus().trim());
            demande.setDateTraitement(Instant.now());
            invaliderJeton(demande);
            demande = demandeIntegrationRepository.save(demande);

            journalAuditService.enregistrer(
                    "REFUSER_DEMANDE_INTEGRATION",
                    "DemandeIntegration",
                    id,
                    acteurEmail,
                    ancienStatut,
                    nouveauStatut,
                    request.getMotifRefus(),
                    demande.getLaboratoire() == null ? null : demande.getLaboratoire().getId()
            );

            DemandeIntegration refusee = demande;
            Map<String, Object> evenementRefus = new HashMap<>();
            evenementRefus.put("demandeId", refusee.getId());
            evenementRefus.put("numero", refusee.getNumero() == null ? "" : refusee.getNumero());
            evenementRefus.put("motif", refusee.getMotifRefus() == null ? "" : refusee.getMotifRefus());
            ApresCommit.executer(() -> {
                try {
                    emailService.envoyerRefusDemandeIntegration(
                            emailsNotification(refusee),
                            nomComplet(refusee),
                            premierNonVide(refusee.getNomLaboratoire(), refusee.getRaisonSociale()),
                            refusee.getNumero(),
                            refusee.getDateTraitement(),
                            refusee.getMotifRefus());
                } catch (Exception emailEx) {
                    log.error("Échec de l'envoi de l'e-mail de refus : {}", emailEx.getMessage());
                }
                notificationEventPublisher.publier("integration-refusee", null, evenementRefus);
            });
            log.info("Demande d'intégration refusée : id={}, motif={}", id, demande.getMotifRefus());
            return toDto(demande, true);
        }

        return approuver(demande, acteurEmail);
    }

    /**
     * Passe la demande en « compléments demandés » et envoie au demandeur un lien
     * à usage unique (valable 14 jours) pour déposer les pièces ou répondre.
     */
    @Transactional
    public DemandeIntegrationDto demanderComplements(Long id, DemanderComplementsRequest request, String acteurEmail) {
        DemandeIntegration demande = charger(id);
        StatutIntegration actuel = StatutIntegration.depuis(demande.getStatut());
        if (!STATUTS_OUVERTS.contains(actuel)) {
            throw new BusinessRuleException("DEMANDE_DEJA_TRAITEE",
                    "Des compléments ne peuvent être demandés que sur une demande en cours");
        }
        Map<String, TypeDocumentIntegration> types = parametrePlateformeService.typesDocumentsParCode();
        List<String> codes = new ArrayList<>();
        for (String code : request.documents() == null ? List.<String>of() : request.documents()) {
            if (!StringUtils.hasText(code) || codes.contains(code.trim())) {
                continue;
            }
            if (!types.containsKey(code.trim())) {
                throw new BusinessRuleException("TYPE_DOCUMENT_INCONNU", "Type de document inconnu : " + code);
            }
            codes.add(code.trim());
        }

        String jeton = genererJeton();
        Instant maintenant = Instant.now();
        demande.setStatut(StatutIntegration.COMPLEMENTS_DEMANDES.name());
        demande.setComplementsDemandes(request.message().trim());
        demande.setDocumentsComplements(codes.isEmpty() ? null : String.join(",", codes));
        demande.setDateDemandeComplements(maintenant);
        demande.setDateReceptionComplements(null);
        demande.setJetonComplementsHash(empreinte(jeton));
        demande.setJetonComplementsExpiration(maintenant.plus(VALIDITE_JETON_COMPLEMENTS));
        demande = demandeIntegrationRepository.save(demande);

        Map<String, Object> apres = new LinkedHashMap<>();
        apres.put("statut", StatutIntegration.COMPLEMENTS_DEMANDES.name());
        apres.put("documents", codes);
        journalAuditService.enregistrer("DEMANDER_COMPLEMENTS_INTEGRATION", "DemandeIntegration", id, acteurEmail,
                json(Map.of("statut", actuel.name())), json(apres),
                request.message().trim(),
                demande.getLaboratoire() == null ? null : demande.getLaboratoire().getId());

        DemandeIntegration concernee = demande;
        List<String> libelles = codes.stream().map(code -> types.get(code).getLibelle()).toList();
        String lien = frontendUrl.replaceAll("/$", "") + "/integration/complements?jeton=" + jeton;
        ApresCommit.executer(() -> {
            try {
                emailService.envoyerDemandeComplements(
                        emailsNotification(concernee),
                        nomComplet(concernee),
                        premierNonVide(concernee.getNomLaboratoire(), concernee.getRaisonSociale()),
                        concernee.getNumero(),
                        concernee.getComplementsDemandes(),
                        libelles,
                        lien,
                        concernee.getJetonComplementsExpiration());
            } catch (Exception emailEx) {
                log.error("Échec de l'envoi de la demande de compléments : {}", emailEx.getMessage());
            }
            notificationEventPublisher.publier("integration-complements-demandes", null,
                    Map.of("demandeId", concernee.getId(), "numero", concernee.getNumero()));
        });
        log.info("Compléments demandés : demande={}, documents={}", id, codes);
        return toDto(demande, true);
    }

    @Transactional(readOnly = true)
    public ComplementsPublicDto consulterComplements(String jeton) {
        DemandeIntegration demande = demandeParJeton(jeton);
        return versComplementsPublic(demande);
    }

    /**
     * Dépôt public des compléments : remplace les documents envoyés, enregistre la
     * réponse écrite et remet la demande en attente de vérification. Le lien ne sert qu'une fois.
     */
    @Transactional
    public ComplementsPublicDto deposerComplements(String jeton, String reponse, Map<String, MultipartFile> fichiers) {
        DemandeIntegration demande = demandeParJeton(jeton);
        Map<String, TypeDocumentIntegration> types = parametrePlateformeService.typesDocumentsParCode();
        List<String> demandes = codesComplements(demande);
        Map<String, MultipartFile> recus = new LinkedHashMap<>();
        for (Map.Entry<String, MultipartFile> entree : (fichiers == null ? Map.<String, MultipartFile>of() : fichiers).entrySet()) {
            if (entree.getValue() == null || entree.getValue().isEmpty()) {
                continue;
            }
            if (!demandes.contains(entree.getKey())) {
                throw new BusinessRuleException("DOCUMENT_NON_DEMANDE",
                        "Ce document n'a pas été demandé : " + entree.getKey());
            }
            validerFichier(types.get(entree.getKey()), entree.getValue());
            recus.put(entree.getKey(), entree.getValue());
        }
        for (String code : demandes) {
            if (!recus.containsKey(code)) {
                throw new BusinessRuleException("DOCUMENT_MANQUANT",
                        "Le document demandé est manquant : " + types.get(code).getLibelle());
            }
        }
        String texteReponse = normaliser(reponse);
        if (recus.isEmpty() && texteReponse == null) {
            throw new BusinessRuleException("REPONSE_OBLIGATOIRE",
                    "Joignez les documents demandés ou répondez au message.");
        }
        if (texteReponse != null && texteReponse.length() > 4000) {
            throw new BusinessRuleException("REPONSE_TROP_LONGUE", "La réponse ne peut pas dépasser 4000 caractères");
        }

        for (Map.Entry<String, MultipartFile> entree : recus.entrySet()) {
            remplacerDocument(demande, types.get(entree.getKey()), entree.getValue());
        }
        Instant maintenant = Instant.now();
        if (texteReponse != null) {
            String precedent = demande.getMessage() == null ? "" : demande.getMessage() + "\n\n";
            demande.setMessage(precedent + "[Réponse du " + DATE_EXPORT.format(maintenant) + "] " + texteReponse);
        }
        demande.setStatut(StatutIntegration.EN_ATTENTE.name());
        demande.setDateReceptionComplements(maintenant);
        invaliderJeton(demande);
        demande = demandeIntegrationRepository.save(demande);

        journalAuditService.enregistrer("RECEVOIR_COMPLEMENTS_INTEGRATION", "DemandeIntegration", demande.getId(),
                demande.getContactEmail(), json(Map.of("statut", StatutIntegration.COMPLEMENTS_DEMANDES.name())),
                json(Map.of("statut", StatutIntegration.EN_ATTENTE.name(), "documents", List.copyOf(recus.keySet()))),
                texteReponse, demande.getLaboratoire() == null ? null : demande.getLaboratoire().getId());

        DemandeIntegration recue = demande;
        ApresCommit.executer(() -> notificationService.notifierRole(RoleUtilisateur.SUPER_ADMINISTRATEUR,
                "COMPLEMENTS_INTEGRATION",
                "Compléments reçus",
                premierNonVide(recue.getNomLaboratoire(), recue.getRaisonSociale()) + " — " + recue.getNumero(),
                lienDemande(recue.getId())));
        log.info("Compléments reçus : demande={}, documents={}", demande.getId(), recus.keySet());
        return versComplementsPublic(demande);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> telechargerDocument(Long demandeId, String type, boolean attachment) {
        DocumentIntegration document = documentIntegrationRepository
                .findByDemande_IdAndTypeDocument(demandeId, type)
                .orElseThrow(() -> new ResourceNotFoundException("DocumentIntegration", "type", type));

        Resource resource = new FileSystemResource(stockageFichierService.cheminAbsolu(document.getCheminStockage()));
        if (!resource.exists()) {
            throw new ResourceNotFoundException("Le fichier " + document.getNomFichier() + " est introuvable");
        }

        String disposition = (attachment ? "attachment" : "inline")
                + "; filename=\"" + document.getNomFichier().replace("\"", "") + "\"";
        MediaType mediaType = MediaType.parseMediaType(
                StringUtils.hasText(document.getTypeMime()) ? document.getTypeMime() : "application/octet-stream");

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .body(resource);
    }

    @Transactional
    public DemandeIntegrationDto ajouterDocument(Long demandeId, String code, MultipartFile fichier, String acteurEmail) {
        DemandeIntegration demande = charger(demandeId);
        StatutIntegration statut = StatutIntegration.depuis(demande.getStatut());
        if (!STATUTS_OUVERTS.contains(statut)) {
            throw new BusinessRuleException("DEMANDE_DEJA_TRAITEE",
                    "Les documents ne peuvent être ajoutés que sur une demande encore en cours");
        }
        TypeDocumentIntegration type = parametrePlateformeService.typesDocumentsParCode().get(code);
        if (type == null) {
            throw new BusinessRuleException("TYPE_DOCUMENT_INCONNU", "Type de document inconnu : " + code);
        }
        validerFichier(type, fichier);
        boolean remplacement = remplacerDocument(demande, type, fichier);
        String nomFichier = fichier.getOriginalFilename() == null ? "" : fichier.getOriginalFilename();
        journalAuditService.enregistrer(
                remplacement ? "REMPLACER_DOCUMENT_INTEGRATION" : "AJOUTER_DOCUMENT_INTEGRATION",
                "DemandeIntegration",
                demandeId,
                acteurEmail,
                remplacement ? json(Map.of("type", code)) : null,
                json(Map.of("type", code, "fichier", nomFichier)),
                null,
                demande.getLaboratoire() == null ? null : demande.getLaboratoire().getId()
        );

        log.info("Document d'intégration ajouté : demande={}, type={}, acteur={}", demandeId, code, acteurEmail);
        return toDto(charger(demandeId), true);
    }

    @Transactional
    public ResultatEnvoiEmail renvoyerInvitation(Long id, String acteurEmail) {
        DemandeIntegration demande = charger(id);
        if (!StatutIntegration.ACCEPTEE.name().equals(demande.getStatut()) || demande.getLaboratoire() == null) {
            throw new BusinessRuleException("INVITATION_IMPOSSIBLE",
                    "L'invitation n'est possible que pour une demande acceptée");
        }
        Utilisateur admin = utilisateurRepository.findByEmail(demande.getContactEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "email", demande.getContactEmail()));
        if (admin.getLaboratoire() == null) {
            admin.setLaboratoire(demande.getLaboratoire());
        }
        ResultatEnvoiEmail resultat = activationCompteService.creerEtEnvoyer(admin);
        journalAuditService.enregistrer(
                "RENVOYER_INVITATION",
                "Utilisateur",
                admin.getId(),
                acteurEmail,
                null,
                json(Map.of("email", admin.getEmail(), "accepte", resultat.accepte())),
                null,
                admin.getLaboratoire() == null ? null : admin.getLaboratoire().getId());
        return resultat;
    }

    private DemandeIntegrationDto approuver(DemandeIntegration demande, String acteurEmail) {
        if (!documentsObligatoiresPresents(demande)) {
            throw new BusinessRuleException("DOCUMENTS_INCOMPLETS",
                    "Tous les documents obligatoires doivent être présents avant l'acceptation");
        }
        if (utilisateurRepository.existsByEmail(demande.getContactEmail())) {
            throw new BusinessRuleException("EMAIL_ADMIN_DEJA_UTILISE",
                    "L'e-mail de l'administrateur est déjà associé à un compte");
        }

        boolean laboratoireCree = false;
        boolean schemaCree = false;
        String nomSchema = null;
        Laboratoire laboratoire = demande.getLaboratoire();
        String ancienStatut = demande.getStatut();

        try {
            // Étape 1 : Créer le laboratoire (si inexistant)
            if (laboratoire == null) {
                String code = slugifier(StringUtils.hasText(demande.getNomLaboratoire())
                        ? demande.getNomLaboratoire() : demande.getRaisonSociale());
                if (laboratoireRepository.existsByCode(code)) {
                    code = code + "_" + demande.getId();
                }
                String nomAffiche = StringUtils.hasText(demande.getNomLaboratoire())
                        ? demande.getNomLaboratoire() : demande.getRaisonSociale();
                laboratoire = laboratoireRepository.save(Laboratoire.builder()
                        .code(code)
                        .raisonSociale(nomAffiche)
                        .ice(demande.getIce())
                        .typeLaboratoire(demande.getTypeLaboratoire())
                        .adresse(demande.getAdresse())
                        .ville(demande.getVille())
                        .region(demande.getRegion())
                        .pays(demande.getPays())
                        .codePostal(demande.getCodePostal())
                        .latitude(demande.getLatitude())
                        .longitude(demande.getLongitude())
                        .telephone(premierNonVide(demande.getTelephoneLaboratoire(), demande.getContactTelephone()))
                        .email(premierNonVide(demande.getEmailLaboratoire(), demande.getContactEmail()))
                        .siteWeb(demande.getSiteWeb())
                        .nomSchema("lims_" + code)
                        .statut(STATUT_ACTIF)
                        .dateCreation(Instant.now())
                        .build());
                demande.setLaboratoire(laboratoire);
                laboratoireCree = true;
            }
            nomSchema = laboratoire.getNomSchema();

            // Étape 2 : Provisionner le schéma (CREATE DATABASE — commit implicite MySQL)
            tenantProvisioner.provisionner(laboratoire, acteurEmail);
            schemaCree = true;

            // Étape 3 : Créer l'administrateur
            Utilisateur admin = creerAdministrateur(demande, laboratoire);
            String lienActivation = activationCompteService.creerJeton(admin);

            // Étape 4 : Mettre à jour la demande (statut ACCEPTEE)
            demande.setStatut(StatutIntegration.ACCEPTEE.name());
            demande.setDateTraitement(Instant.now());
            demande.setMotifRefus(null);
            invaliderJeton(demande);
            demande = demandeIntegrationRepository.save(demande);

            // Étape 5 : Emails (non-critique - échec = pas de compensation)
            DemandeIntegration acceptee = demande;
            String nomLabo = premierNonVide(demande.getNomLaboratoire(), demande.getRaisonSociale());
            String nomAdmin = nomComplet(demande);
            int ttlHeures = activationCompteService.getDureeHeures();
            Laboratoire laboratoireAccepte = laboratoire;
            Map<String, Object> evenementAcceptation = new HashMap<>();
            evenementAcceptation.put("demandeId", acceptee.getId());
            evenementAcceptation.put("numero", acceptee.getNumero() == null ? "" : acceptee.getNumero());
            evenementAcceptation.put("laboratoireId", laboratoireAccepte.getId() == null ? 0L : laboratoireAccepte.getId());
            evenementAcceptation.put("code", laboratoireAccepte.getCode() == null ? "" : laboratoireAccepte.getCode());
            String schemaAccepte = laboratoireAccepte.getNomSchema() == null ? "" : laboratoireAccepte.getNomSchema();

            journalAuditService.enregistrer(
                    "ACCEPTER_DEMANDE_INTEGRATION",
                    "DemandeIntegration",
                    demande.getId(),
                    acteurEmail,
                    ancienStatut,
                    StatutIntegration.ACCEPTEE.name(),
                    null,
                    laboratoire.getId()
            );

            ApresCommit.executer(() -> {
                try {
                    emailService.envoyerAcceptationDemandeIntegration(
                            emailsNotification(acceptee),
                            nomAdmin,
                            nomLabo,
                            acceptee.getNumero(),
                            acceptee.getVille(),
                            acceptee.getDateTraitement());
                } catch (Exception emailEx) {
                    log.error("Échec de l'envoi de l'email d'acceptation : {}", emailEx.getMessage());
                }
                try {
                    emailService.envoyerInvitationAdministrateur(
                            acceptee.getContactEmail(),
                            nomAdmin,
                            nomLabo,
                            lienActivation,
                            ttlHeures);
                } catch (Exception emailEx) {
                    log.error("Échec de l'envoi de l'email d'invitation : {}", emailEx.getMessage());
                }
                notificationEventPublisher.publier("integration-acceptee", schemaAccepte, evenementAcceptation);
            });

            log.info("Demande d'intégration approuvée : id={}, labo={}, schema={}",
                    demande.getId(), laboratoire.getCode(), laboratoire.getNomSchema());
            return toDto(demande, true);

        } catch (RuntimeException ex) {
            log.error("Erreur lors de l'acceptation de la demande {} (laboratoireCree={}, schemaCree={}) : {}",
                    demande.getId(), laboratoireCree, schemaCree, ex.getMessage());

            // Compensation : ne nettoyer que ce que cet appel a créé
            if (schemaCree && nomSchema != null) {
                try {
                    tenantProvisioner.deprovisionner(nomSchema, acteurEmail);
                    log.info("Compensation : schéma {} supprimé", nomSchema);
                } catch (Exception compEx) {
                    log.error("Échec de la compensation du schéma {} : {}", nomSchema, compEx.getMessage());
                }
            }
            // Le laboratoire et l'administrateur créés dans cette transaction sont
            // annulés par le rollback Spring. Aucun delete manuel : un laboratoire
            // qui existait avant l'appel (laboratoireCree == false) est conservé.
            throw ex;
        }
    }

    private Utilisateur creerAdministrateur(DemandeIntegration demande, Laboratoire laboratoire) {
        Role role = roleRepository.findByCode(RoleUtilisateur.ADMINISTRATEUR.name())
                .orElseThrow(() -> new ResourceNotFoundException("Role", "code", RoleUtilisateur.ADMINISTRATEUR.name()));

        String matricule = matriculeGeneratorService.genererMatricule(RoleUtilisateur.ADMINISTRATEUR);

        String motDePasseTemporaire = UUID.randomUUID().toString() + "A1a";
        Utilisateur admin = Utilisateur.builder()
                .matricule(matricule)
                .nom(demande.getContactNom())
                .prenom(demande.getContactPrenom())
                .email(demande.getContactEmail())
                .telephone(demande.getContactTelephone())
                .cin(demande.getContactCin())
                .fonction(demande.getContactFonction())
                .motDePasseHash(passwordEncoder.encode(motDePasseTemporaire))
                .mustChangePassword(true)
                .actif(false)
                .role(role)
                .laboratoire(laboratoire)
                .build();
        admin = utilisateurRepository.save(admin);
        log.info("Administrateur laboratoire créé (inactif, en attente d'activation) : userId={}, labo={}",
                admin.getId(), laboratoire.getCode());
        return admin;
    }

    /** Remplace le document du même type s'il existe. Retourne vrai s'il y a eu remplacement. */
    private boolean remplacerDocument(DemandeIntegration demande, TypeDocumentIntegration type, MultipartFile fichier) {
        boolean remplacement = documentIntegrationRepository.findByDemande_IdAndTypeDocument(demande.getId(), type.getCode())
                .map(existant -> {
                    stockageFichierService.supprimer(existant.getCheminStockage());
                    demande.getDocuments().remove(existant);
                    documentIntegrationRepository.delete(existant);
                    documentIntegrationRepository.flush();
                    return true;
                })
                .orElse(false);
        enregistrerDocument(demande, type, fichier);
        return remplacement;
    }

    private void enregistrerDocument(DemandeIntegration demande, TypeDocumentIntegration type, MultipartFile fichier) {
        String extension = extension(fichier.getOriginalFilename());
        String objectName = "integration/" + demande.getId() + "/" + type.getCode() + "_"
                + UUID.randomUUID().toString().replace("-", "") + "." + extension;
        try (var input = fichier.getInputStream()) {
            String chemin = stockageFichierService.televerser(
                    objectName, input, fichier.getSize(), fichier.getContentType());
            DocumentIntegration document = DocumentIntegration.builder()
                    .demande(demande)
                    .typeDocument(type.getCode())
                    .nomFichier(fichier.getOriginalFilename())
                    .typeMime(fichier.getContentType() != null ? fichier.getContentType() : "application/octet-stream")
                    .taille(fichier.getSize())
                    .cheminStockage(chemin)
                    .dateAjout(Instant.now())
                    .build();
            documentIntegrationRepository.save(document);
        } catch (IOException ex) {
            throw new BusinessRuleException("UPLOAD_DOCUMENT_ECHOUE",
                    "Impossible d'enregistrer le document " + type.getLibelle());
        }
    }

    private void validerUnicite(SoumettreDemandeIntegrationRequest request) {
        String email = request.getAdminEmail().trim().toLowerCase();
        for (StatutIntegration statut : STATUTS_OUVERTS) {
            if (demandeIntegrationRepository.existsByContactEmailAndStatut(email, statut.name())) {
                throw new BusinessRuleException("DEMANDE_DEJA_EN_ATTENTE",
                        "Une demande d'intégration est déjà en cours pour cet e-mail");
            }
        }
        if (utilisateurRepository.existsByEmail(email)) {
            throw new BusinessRuleException("EMAIL_DEJA_UTILISE",
                    "Cet e-mail est déjà associé à un compte LabFlow");
        }
        if (StringUtils.hasText(request.getIce())) {
            String ice = request.getIce().trim();
            boolean enCours = STATUTS_OUVERTS.stream()
                    .anyMatch(statut -> demandeIntegrationRepository.existsByIceAndStatut(ice, statut.name()));
            if (laboratoireRepository.existsByIce(ice) || enCours) {
                throw new BusinessRuleException("ICE_DEJA_UTILISE",
                        "Cet identifiant ICE est déjà associé à un laboratoire ou une demande en cours");
            }
        }
    }

    /**
     * Vérifie les fichiers déposés : chaque document obligatoire est présent,
     * aucun fichier ne correspond à un type inconnu ou inactif.
     */
    private Map<String, MultipartFile> validerDocuments(List<TypeDocumentIntegration> actifs,
                                                        Map<String, MultipartFile> fichiers) {
        Map<String, TypeDocumentIntegration> parCode = actifs.stream()
                .collect(Collectors.toMap(TypeDocumentIntegration::getCode, t -> t, (a, b) -> a, LinkedHashMap::new));
        for (String code : fichiers.keySet()) {
            if (!parCode.containsKey(code)) {
                throw new BusinessRuleException("TYPE_DOCUMENT_INCONNU",
                        "Ce document n'est pas demandé pour l'intégration : " + code);
            }
        }
        Map<String, MultipartFile> retenus = new LinkedHashMap<>();
        for (TypeDocumentIntegration type : actifs) {
            MultipartFile fichier = fichiers.get(type.getCode());
            if (fichier == null || fichier.isEmpty()) {
                if (type.isObligatoire()) {
                    throw new BusinessRuleException("DOCUMENT_MANQUANT",
                            "Le document obligatoire est manquant : " + type.getLibelle());
                }
                continue;
            }
            validerFichier(type, fichier);
            retenus.put(type.getCode(), fichier);
        }
        return retenus;
    }

    private void validerFichier(TypeDocumentIntegration type, MultipartFile fichier) {
        String libelle = type == null ? "Document" : type.getLibelle();
        if (fichier == null || fichier.isEmpty()) {
            throw new BusinessRuleException("DOCUMENT_MANQUANT",
                    "Le document obligatoire est manquant : " + libelle);
        }
        if (fichier.getSize() > tailleMaxOctets) {
            throw new BusinessRuleException("DOCUMENT_TROP_VOLUMINEUX",
                    libelle + " dépasse la taille maximale de 10 Mo");
        }
        String extension = extension(fichier.getOriginalFilename());
        if (!EXTENSIONS_AUTORISEES.contains(extension)) {
            throw new BusinessRuleException("FORMAT_DOCUMENT_INVALIDE",
                    libelle + " : formats acceptés PDF, JPG, PNG, WEBP");
        }
        String mime = fichier.getContentType() == null ? "" : fichier.getContentType().toLowerCase(Locale.ROOT);
        if (StringUtils.hasText(mime) && !MIME_AUTORISES.contains(mime)
                && !"application/octet-stream".equals(mime)) {
            throw new BusinessRuleException("FORMAT_DOCUMENT_INVALIDE",
                    libelle + " : type MIME non autorisé (" + mime + ")");
        }
    }

    private boolean documentsObligatoiresPresents(DemandeIntegration demande) {
        Set<String> presents = documentIntegrationRepository.findByDemande_Id(demande.getId()).stream()
                .map(DocumentIntegration::getTypeDocument)
                .collect(Collectors.toSet());
        return parametrePlateformeService.typesDocumentsActifs().stream()
                .filter(TypeDocumentIntegration::isObligatoire)
                .allMatch(type -> presents.contains(type.getCode()));
    }

    private DemandeIntegration demandeParJeton(String jeton) {
        if (!StringUtils.hasText(jeton) || jeton.length() > 200) {
            throw new BusinessRuleException("LIEN_COMPLEMENTS_INVALIDE", "Ce lien n'est pas valide.");
        }
        DemandeIntegration demande = demandeIntegrationRepository.findByJetonComplementsHash(empreinte(jeton.trim()))
                .orElseThrow(() -> new BusinessRuleException("LIEN_COMPLEMENTS_INVALIDE",
                        "Ce lien n'est pas valide ou a déjà été utilisé."));
        if (StatutIntegration.depuis(demande.getStatut()) != StatutIntegration.COMPLEMENTS_DEMANDES
                || demande.getJetonComplementsExpiration() == null
                || Instant.now().isAfter(demande.getJetonComplementsExpiration())) {
            throw new BusinessRuleException("LIEN_COMPLEMENTS_EXPIRE",
                    "Ce lien a expiré. Contactez l'équipe LabFlow pour recevoir un nouveau lien.");
        }
        return demande;
    }

    private ComplementsPublicDto versComplementsPublic(DemandeIntegration demande) {
        Map<String, TypeDocumentIntegration> types = parametrePlateformeService.typesDocumentsParCode();
        Set<String> presents = documentIntegrationRepository.findByDemande_Id(demande.getId()).stream()
                .map(DocumentIntegration::getTypeDocument)
                .collect(Collectors.toSet());
        List<ComplementsPublicDto.DocumentDemande> documents = codesComplements(demande).stream()
                .filter(types::containsKey)
                .map(code -> new ComplementsPublicDto.DocumentDemande(code, types.get(code).getLibelle(),
                        types.get(code).getRaison(), presents.contains(code)))
                .toList();
        return new ComplementsPublicDto(
                demande.getNumero(),
                premierNonVide(demande.getNomLaboratoire(), demande.getRaisonSociale()),
                demande.getComplementsDemandes(),
                demande.getDateDemandeComplements(),
                demande.getJetonComplementsExpiration(),
                documents);
    }

    private static List<String> codesComplements(DemandeIntegration demande) {
        if (!StringUtils.hasText(demande.getDocumentsComplements())) {
            return List.of();
        }
        return Arrays.stream(demande.getDocumentsComplements().split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private static void invaliderJeton(DemandeIntegration demande) {
        demande.setJetonComplementsHash(null);
        demande.setJetonComplementsExpiration(null);
    }

    private DemandeIntegration charger(Long id) {
        return demandeIntegrationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DemandeIntegration", "id", id));
    }

    private DemandeIntegrationDto toDto(DemandeIntegration d, boolean avecDocuments) {
        Laboratoire lab = d.getLaboratoire();
        DemandeIntegrationDto dto = DemandeIntegrationDto.builder()
                .id(d.getId())
                .numero(d.getNumero())
                .statut(StatutIntegration.depuis(d.getStatut()))
                .dateSoumission(d.getDateDemande())
                .dateTraitement(d.getDateTraitement())
                .motifRefus(d.getMotifRefus())
                .commentaireAdmin(d.getMotifRefus())
                .nomLaboratoire(premierNonVide(d.getNomLaboratoire(), d.getRaisonSociale()))
                .raisonSociale(d.getRaisonSociale())
                .typeLaboratoire(d.getTypeLaboratoire())
                .typesLaboratoire(extraireTypes(d.getTypeLaboratoire()))
                .ice(d.getIce())
                .telephoneLaboratoire(d.getTelephoneLaboratoire())
                .emailLaboratoire(d.getEmailLaboratoire())
                .siteWeb(d.getSiteWeb())
                .informationsComplementaires(d.getMessage())
                .adresse(d.getAdresse())
                .ville(d.getVille())
                .region(d.getRegion())
                .pays(d.getPays())
                .codePostal(d.getCodePostal())
                .latitude(d.getLatitude())
                .longitude(d.getLongitude())
                .adminNom(d.getContactNom())
                .adminPrenom(d.getContactPrenom())
                .adminEmail(d.getContactEmail())
                .adminTelephone(d.getContactTelephone())
                .adminFonction(d.getContactFonction())
                .adminCin(d.getContactCin())
                .emailRepresentant(d.getContactEmail())
                .nomRepresentant(nomComplet(d))
                .telephoneRepresentant(d.getContactTelephone())
                .message(d.getMessage())
                .laboratoireId(lab != null ? lab.getId() : null)
                .laboratoireCode(lab != null ? lab.getCode() : null)
                .nomSchema(lab != null ? lab.getNomSchema() : null)
                .complementsDemandes(d.getComplementsDemandes())
                .documentsComplements(new ArrayList<>(codesComplements(d)))
                .dateDemandeComplements(d.getDateDemandeComplements())
                .dateReceptionComplements(d.getDateReceptionComplements())
                .historique(construireHistorique(d))
                .build();

        List<DocumentIntegration> documents = documentIntegrationRepository.findByDemande_Id(d.getId());
        Set<String> presents = documents.stream().map(DocumentIntegration::getTypeDocument).collect(Collectors.toSet());
        if (avecDocuments) {
            Map<String, TypeDocumentIntegration> types = parametrePlateformeService.typesDocumentsParCode();
            dto.setDocuments(documents.stream().map(doc -> toDocumentDto(doc, types.get(doc.getTypeDocument()))).toList());
            List<TypeDocumentIntegrationDto> attendus = types.values().stream()
                    .filter(type -> type.isActif() || presents.contains(type.getCode()))
                    .sorted(Comparator.comparingInt(TypeDocumentIntegration::getOrdre)
                            .thenComparing(TypeDocumentIntegration::getLibelle))
                    .map(TypeDocumentIntegrationDto::from)
                    .toList();
            dto.setDocumentsAttendus(new ArrayList<>(attendus));
        }
        dto.setNombreDocuments(documents.size());
        dto.setDossierComplet(parametrePlateformeService.typesDocumentsActifs().stream()
                .filter(TypeDocumentIntegration::isObligatoire)
                .allMatch(type -> presents.contains(type.getCode())));
        return dto;
    }

    private List<HistoriqueDecisionDto> construireHistorique(DemandeIntegration demande) {
        List<HistoriqueDecisionDto> evenements = new ArrayList<>();
        evenements.add(HistoriqueDecisionDto.builder()
                .evenement("Demande reçue")
                .date(demande.getDateDemande())
                .detail(null)
                .build());
        if (demande.getDateDemandeComplements() != null) {
            evenements.add(HistoriqueDecisionDto.builder()
                    .evenement("Compléments demandés")
                    .date(demande.getDateDemandeComplements())
                    .detail(demande.getComplementsDemandes())
                    .build());
        }
        if (demande.getDateReceptionComplements() != null) {
            evenements.add(HistoriqueDecisionDto.builder()
                    .evenement("Compléments reçus")
                    .date(demande.getDateReceptionComplements())
                    .detail(null)
                    .build());
        }
        if (demande.getDateTraitement() != null) {
            StatutIntegration decision = StatutIntegration.depuis(demande.getStatut());
            String evenement = decision == StatutIntegration.ACCEPTEE
                    ? "Demande acceptée"
                    : decision == StatutIntegration.REFUSEE
                    ? "Demande refusée"
                    : "Demande traitée";
            evenements.add(HistoriqueDecisionDto.builder()
                    .evenement(evenement)
                    .date(demande.getDateTraitement())
                    .detail(demande.getMotifRefus())
                    .build());
        }
        return evenements;
    }

    private DocumentIntegrationDto toDocumentDto(DocumentIntegration doc, TypeDocumentIntegration type) {
        return DocumentIntegrationDto.builder()
                .id(doc.getId())
                .typeDocument(doc.getTypeDocument())
                .libelle(type == null ? doc.getTypeDocument() : type.getLibelle())
                .raison(type == null ? null : type.getRaison())
                .nomFichier(doc.getNomFichier())
                .typeMime(doc.getTypeMime())
                .taille(doc.getTaille())
                .dateAjout(doc.getDateAjout())
                .build();
    }

    private String lienDemande(Long id) {
        return "/app/gestion-laboratoires?onglet=demandes&demande=" + id;
    }

    private static String libelleStatut(StatutIntegration statut) {
        if (statut == null) {
            return "";
        }
        return switch (statut) {
            case EN_ATTENTE -> "En attente";
            case COMPLEMENTS_DEMANDES -> "Compléments demandés";
            case ACCEPTEE -> "Acceptée";
            case REFUSEE -> "Refusée";
        };
    }

    private static Sort triDemandes(String tri, String ordre) {
        Set<String> tris = Set.of("dateDemande", "numero", "nomLaboratoire", "ville", "statut", "contactEmail");
        String champ = tris.contains(tri) ? tri : "dateDemande";
        Sort.Direction direction = "asc".equalsIgnoreCase(ordre) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, champ);
    }

    private static String terme(String recherche) {
        return StringUtils.hasText(recherche)
                ? recherche.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
                : null;
    }

    private static String genererJeton() {
        byte[] octets = new byte[32];
        ALEA.nextBytes(octets);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(octets);
    }

    static String empreinte(String jeton) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(jeton.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 non disponible", ex);
        }
    }

    private String json(Object valeur) {
        try {
            return objectMapper.writeValueAsString(valeur);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    private static String slugifier(String valeur) {
        String slug = valeur == null ? "lab" : valeur.toLowerCase()
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_|_$", "");
        if (slug.isBlank()) {
            slug = "lab";
        }
        return slug.length() > 40 ? slug.substring(0, 40) : slug;
    }

    private static String extension(String nomFichier) {
        if (!StringUtils.hasText(nomFichier) || !nomFichier.contains(".")) {
            return "";
        }
        return nomFichier.substring(nomFichier.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private static String normaliser(String valeur) {
        return StringUtils.hasText(valeur) ? valeur.trim() : null;
    }

    private static String premierNonVide(String... valeurs) {
        if (valeurs == null) {
            return null;
        }
        for (String valeur : valeurs) {
            if (StringUtils.hasText(valeur)) {
                return valeur;
            }
        }
        return null;
    }

    private static String joindreTypes(List<String> types) {
        if (types == null || types.isEmpty()) {
            return null;
        }
        return types.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .collect(Collectors.joining(", "));
    }

    private static List<String> extraireTypes(String brut) {
        if (!StringUtils.hasText(brut)) {
            return List.of();
        }
        return Arrays.stream(brut.split("\\s*,\\s*"))
                .filter(StringUtils::hasText)
                .toList();
    }

    private static List<String> emailsNotification(DemandeIntegration demande) {
        LinkedHashSet<String> emails = new LinkedHashSet<>();
        if (StringUtils.hasText(demande.getContactEmail())) {
            emails.add(demande.getContactEmail().trim().toLowerCase());
        }
        if (StringUtils.hasText(demande.getEmailLaboratoire())) {
            emails.add(demande.getEmailLaboratoire().trim().toLowerCase());
        }
        return List.copyOf(emails);
    }

    private static String nomComplet(DemandeIntegration d) {
        if (StringUtils.hasText(d.getContactPrenom())) {
            return d.getContactPrenom() + " " + d.getContactNom();
        }
        return d.getContactNom();
    }
}
