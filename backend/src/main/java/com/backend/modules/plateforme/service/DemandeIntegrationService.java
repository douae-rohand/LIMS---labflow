package com.backend.modules.plateforme.service;

import com.backend.common.exception.BusinessRuleException;
import com.backend.common.exception.ResourceNotFoundException;
import com.backend.common.tenant.TenantProvisioner;
import com.backend.common.util.ApresCommit;
import com.backend.integration.minio.StockageFichierService;
import com.backend.integration.sendgrid.EmailService;
import com.backend.integration.sendgrid.ResultatEnvoiEmail;
import com.backend.modules.auth.service.ActivationCompteService;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
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

    @Value("${app.integration.max-file-size-bytes:10485760}")
    private long tailleMaxOctets;

    public List<TypeDocumentIntegrationDto> listerDocumentsRequis() {
        return Arrays.stream(TypeDocumentIntegration.values())
                .map(TypeDocumentIntegrationDto::from)
                .toList();
    }

    @Transactional
    public DemandeIntegrationDto soumettre(SoumettreDemandeIntegrationRequest request,
                                           Map<TypeDocumentIntegration, MultipartFile> fichiers) {
        validerUnicite(request);
        validerDocuments(fichiers);

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

        for (TypeDocumentIntegration type : TypeDocumentIntegration.values()) {
            MultipartFile fichier = fichiers.get(type);
            enregistrerDocument(demande, type, fichier);
        }

        DemandeIntegration confirmee = demande;
        ApresCommit.executer(() -> emailService.envoyerConfirmationDemandeIntegration(
                emailsNotification(confirmee),
                nomComplet(confirmee),
                premierNonVide(confirmee.getNomLaboratoire(), confirmee.getRaisonSociale()),
                confirmee.getNumero(),
                confirmee.getVille(),
                TypeDocumentIntegration.values().length,
                confirmee.getDateDemande()));
        log.info("Demande d'intégration soumise : id={}, numero={}, labo={}",
                demande.getId(), demande.getNumero(), demande.getNomLaboratoire());
        return toDto(demande, true);
    }

    @Transactional(readOnly = true)
    public Page<DemandeIntegrationDto> lister(StatutIntegration statut, Pageable pageable) {
        Page<DemandeIntegration> page = (statut != null)
                ? demandeIntegrationRepository.findByStatut(statut.name(), pageable)
                : demandeIntegrationRepository.findAll(pageable);
        return page.map(d -> toDto(d, false));
    }

    @Transactional(readOnly = true)
    public DemandeIntegrationDto trouver(Long id) {
        return toDto(charger(id), true);
    }

    @Transactional
    public DemandeIntegrationDto traiter(Long id, TraiterDemandeIntegrationRequest request) {
        DemandeIntegration demande = charger(id);
        StatutIntegration actuel = enumOuNull(StatutIntegration.class, demande.getStatut());

        if (actuel == StatutIntegration.APPROUVEE || actuel == StatutIntegration.REJETEE) {
            throw new BusinessRuleException("DEMANDE_DEJA_TRAITEE",
                    "Cette demande a déjà été traitée (" + actuel + ")");
        }
        if (request.getDecision() != StatutIntegration.APPROUVEE
                && request.getDecision() != StatutIntegration.REJETEE) {
            throw new BusinessRuleException("DECISION_INVALIDE",
                    "La décision doit être APPROUVEE ou REJETEE");
        }

        if (request.getDecision() == StatutIntegration.REJETEE) {
            if (!StringUtils.hasText(request.getMotifRefus())) {
                throw new BusinessRuleException("MOTIF_REFUS_OBLIGATOIRE",
                        "Le motif du refus est obligatoire");
            }
            demande.setStatut(StatutIntegration.REJETEE.name());
            demande.setMotifRefus(request.getMotifRefus().trim());
            demande.setDateTraitement(Instant.now());
            demande = demandeIntegrationRepository.save(demande);
            DemandeIntegration refusee = demande;
            ApresCommit.executer(() -> emailService.envoyerRefusDemandeIntegration(
                    emailsNotification(refusee),
                    nomComplet(refusee),
                    premierNonVide(refusee.getNomLaboratoire(), refusee.getRaisonSociale()),
                    refusee.getNumero(),
                    refusee.getDateTraitement(),
                    refusee.getMotifRefus()));
            log.info("Demande d'intégration refusée : id={}, motif={}", id, demande.getMotifRefus());
            return toDto(demande, true);
        }

        return approuver(demande);
    }

    public ResponseEntity<Resource> telechargerDocument(Long demandeId, TypeDocumentIntegration type, boolean attachment) {
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
    public ResultatEnvoiEmail renvoyerInvitation(Long id) {
        DemandeIntegration demande = charger(id);
        if (!StatutIntegration.APPROUVEE.name().equals(demande.getStatut()) || demande.getLaboratoire() == null) {
            throw new BusinessRuleException("INVITATION_IMPOSSIBLE",
                    "L'invitation n'est possible que pour une demande approuvée");
        }
        Utilisateur admin = utilisateurRepository.findByEmail(demande.getContactEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "email", demande.getContactEmail()));
        if (admin.getLaboratoire() == null) {
            admin.setLaboratoire(demande.getLaboratoire());
        }
        return activationCompteService.creerEtEnvoyer(admin);
    }

    private DemandeIntegrationDto approuver(DemandeIntegration demande) {
        if (documentIntegrationRepository.countByDemande_Id(demande.getId()) != TypeDocumentIntegration.values().length) {
            throw new BusinessRuleException("DOCUMENTS_INCOMPLETS",
                    "Les 6 documents obligatoires doivent être présents avant l'acceptation");
        }
        if (utilisateurRepository.existsByEmail(demande.getContactEmail())) {
            throw new BusinessRuleException("EMAIL_ADMIN_DEJA_UTILISE",
                    "L'e-mail de l'administrateur est déjà associé à un compte");
        }

        Laboratoire laboratoire = demande.getLaboratoire();
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
        }

        try {
            tenantProvisioner.provisionner(laboratoire);
        } catch (RuntimeException ex) {
            log.error("Provisionnement tenant échoué pour demande {} : {}", demande.getId(), ex.getMessage());
            throw new BusinessRuleException("PROVISIONNEMENT_TENANT_ECHOUE",
                    "La création de l'environnement du laboratoire a échoué : " + ex.getMessage());
        }

        Utilisateur admin = creerAdministrateur(demande, laboratoire);
        String lienActivation = activationCompteService.creerJeton(admin);

        demande.setStatut(StatutIntegration.APPROUVEE.name());
        demande.setDateTraitement(Instant.now());
        demande.setMotifRefus(null);
        demande = demandeIntegrationRepository.save(demande);

        DemandeIntegration acceptee = demande;
        String nomLabo = premierNonVide(demande.getNomLaboratoire(), demande.getRaisonSociale());
        String nomAdmin = nomComplet(demande);
        int ttlHeures = activationCompteService.getDureeHeures();
        ApresCommit.executer(() -> {
            emailService.envoyerAcceptationDemandeIntegration(
                    emailsNotification(acceptee),
                    nomAdmin,
                    nomLabo,
                    acceptee.getNumero(),
                    acceptee.getVille(),
                    acceptee.getDateTraitement());
            emailService.envoyerInvitationAdministrateur(
                    acceptee.getContactEmail(),
                    nomAdmin,
                    nomLabo,
                    lienActivation,
                    ttlHeures);
        });

        log.info("Demande d'intégration approuvée : id={}, labo={}, schema={}",
                demande.getId(), laboratoire.getCode(), laboratoire.getNomSchema());
        return toDto(demande, true);
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

    private void enregistrerDocument(DemandeIntegration demande, TypeDocumentIntegration type, MultipartFile fichier) {
        String extension = extension(fichier.getOriginalFilename());
        String objectName = "integration/" + demande.getId() + "/" + type.name() + "_"
                + UUID.randomUUID().toString().replace("-", "") + "." + extension;
        try (var input = fichier.getInputStream()) {
            String chemin = stockageFichierService.televerser(
                    objectName, input, fichier.getSize(), fichier.getContentType());
            DocumentIntegration document = DocumentIntegration.builder()
                    .demande(demande)
                    .typeDocument(type)
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
        if (demandeIntegrationRepository.existsByContactEmailAndStatut(email, StatutIntegration.EN_ATTENTE.name())) {
            throw new BusinessRuleException("DEMANDE_DEJA_EN_ATTENTE",
                    "Une demande d'intégration est déjà en attente pour cet e-mail");
        }
        if (utilisateurRepository.existsByEmail(email)) {
            throw new BusinessRuleException("EMAIL_DEJA_UTILISE",
                    "Cet e-mail est déjà associé à un compte LabFlow");
        }
        if (StringUtils.hasText(request.getIce())) {
            String ice = request.getIce().trim();
            if (laboratoireRepository.existsByIce(ice)
                    || demandeIntegrationRepository.existsByIceAndStatut(ice, StatutIntegration.EN_ATTENTE.name())) {
                throw new BusinessRuleException("ICE_DEJA_UTILISE",
                        "Cet identifiant ICE est déjà associé à un laboratoire ou une demande en cours");
            }
        }
    }

    private void validerDocuments(Map<TypeDocumentIntegration, MultipartFile> fichiers) {
        if (fichiers == null) {
            throw new BusinessRuleException("DOCUMENTS_OBLIGATOIRES",
                    "Les 6 documents obligatoires doivent être joints à la demande");
        }
        for (TypeDocumentIntegration type : TypeDocumentIntegration.values()) {
            MultipartFile fichier = fichiers.get(type);
            if (fichier == null || fichier.isEmpty()) {
                throw new BusinessRuleException("DOCUMENT_MANQUANT",
                        "Le document obligatoire est manquant : " + type.getLibelle());
            }
            if (fichier.getSize() > tailleMaxOctets) {
                throw new BusinessRuleException("DOCUMENT_TROP_VOLUMINEUX",
                        type.getLibelle() + " dépasse la taille maximale de 10 Mo");
            }
            String extension = extension(fichier.getOriginalFilename());
            if (!EXTENSIONS_AUTORISEES.contains(extension)) {
                throw new BusinessRuleException("FORMAT_DOCUMENT_INVALIDE",
                        type.getLibelle() + " : formats acceptés PDF, JPG, PNG, WEBP");
            }
            String mime = fichier.getContentType() == null ? "" : fichier.getContentType().toLowerCase();
            if (StringUtils.hasText(mime) && !MIME_AUTORISES.contains(mime)
                    && !"application/octet-stream".equals(mime)) {
                throw new BusinessRuleException("FORMAT_DOCUMENT_INVALIDE",
                        type.getLibelle() + " : type MIME non autorisé (" + mime + ")");
            }
        }
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
                .statut(enumOuNull(StatutIntegration.class, d.getStatut()))
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
                .build();

        if (avecDocuments) {
            dto.setDocuments(documentIntegrationRepository.findByDemande_Id(d.getId()).stream()
                    .map(this::toDocumentDto)
                    .toList());
        }
        return dto;
    }

    private DocumentIntegrationDto toDocumentDto(DocumentIntegration doc) {
        return DocumentIntegrationDto.builder()
                .id(doc.getId())
                .typeDocument(doc.getTypeDocument())
                .libelle(doc.getTypeDocument().getLibelle())
                .raison(doc.getTypeDocument().getRaison())
                .nomFichier(doc.getNomFichier())
                .typeMime(doc.getTypeMime())
                .taille(doc.getTaille())
                .dateAjout(doc.getDateAjout())
                .build();
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

    private static <E extends Enum<E>> E enumOuNull(Class<E> type, String valeur) {
        if (valeur == null) {
            return null;
        }
        try {
            return Enum.valueOf(type, valeur);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
