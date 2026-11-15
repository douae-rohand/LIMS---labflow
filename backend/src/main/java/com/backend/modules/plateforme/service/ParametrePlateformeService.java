package com.backend.modules.plateforme.service;

import com.backend.common.audit.JournalAuditService;
import com.backend.common.exception.BusinessRuleException;
import com.backend.common.exception.ResourceNotFoundException;
import com.backend.modules.plateforme.dto.ParametresPlateformeDtos.CreerTypeLaboratoire;
import com.backend.modules.plateforme.dto.ParametresPlateformeDtos.EnregistrerTypeDocument;
import com.backend.modules.plateforme.dto.ParametresPlateformeDtos.Commission;
import com.backend.modules.plateforme.dto.ParametresPlateformeDtos.Identite;
import com.backend.modules.plateforme.dto.ParametresPlateformeDtos.ModifierTypeLaboratoire;
import com.backend.modules.plateforme.dto.ParametresPlateformeDtos.ParametresPublics;
import com.backend.modules.plateforme.dto.ParametresPlateformeDtos.PolitiqueMotDePasse;
import com.backend.modules.plateforme.dto.ParametresPlateformeDtos.Securite;
import com.backend.modules.plateforme.dto.ParametresPlateformeDtos.TypeLaboratoire;
import com.backend.modules.plateforme.dto.TypeDocumentIntegrationDto;
import com.backend.modules.plateforme.entity.ParametrePlateforme;
import com.backend.modules.plateforme.entity.ReferentielTypeLaboratoire;
import com.backend.modules.plateforme.entity.TypeDocumentIntegration;
import com.backend.modules.plateforme.repository.ParametrePlateformeRepository;
import com.backend.modules.plateforme.repository.ReferentielTypeLaboratoireRepository;
import com.backend.modules.plateforme.repository.TypeDocumentIntegrationRepository;
import com.backend.modules.utilisateur.entity.RoleUtilisateur;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Paramètres globaux de la plateforme : sécurité, documents d'intégration
 * et types de laboratoire. Les paramètres de sécurité sont lus à chaque connexion :
 * ils sont gardés en mémoire une minute au plus et rechargés après modification.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParametrePlateformeService {

    static final String CLE_ROLES_2FA = "securite.2fa.roles";
    static final String CLE_LONGUEUR_MDP = "securite.mdp.longueur_min";
    static final String CLE_SPECIAL_MDP = "securite.mdp.exiger_special";
    static final String CLE_DUREE_SESSION = "securite.session.duree_jours";
    static final String CLE_NOM = "plateforme.nom";
    static final String CLE_EMAIL_CONTACT = "plateforme.email_contact";
    static final String CLE_TELEPHONE = "plateforme.telephone";
    static final String CLE_ADRESSE = "plateforme.adresse";
    static final String CLE_EXPEDITEUR = "email.expediteur_nom";
    static final String CLE_COMMISSION = "plateforme.commission.taux";

    public static final String NOM_PLATEFORME_DEFAUT = "LabFlow";
    public static final String EXPEDITEUR_DEFAUT = "LabFlow LIMS";
    public static final BigDecimal COMMISSION_DEFAUT = new BigDecimal("5.00");

    public static final int LONGUEUR_MDP_MIN = 10;
    private static final int DUREE_SESSION_DEFAUT = 7;
    private static final Duration DUREE_CACHE = Duration.ofMinutes(1);

    private final ParametrePlateformeRepository parametreRepository;
    private final TypeDocumentIntegrationRepository typeDocumentRepository;
    private final ReferentielTypeLaboratoireRepository typeLaboratoireRepository;
    private final JournalAuditService journalAuditService;
    private final ObjectMapper objectMapper;
    private final com.backend.common.tenant.TenantExecutor tenantExecutor;

    private volatile Securite securiteEnCache;
    private volatile Identite identiteEnCache;
    private volatile Instant chargementIdentite = Instant.EPOCH;
    private volatile BigDecimal commissionEnCache;
    private volatile Instant chargementCommission = Instant.EPOCH;
    private volatile Instant chargementCache = Instant.EPOCH;

    // -------------------------------------------------------------------------
    // Sécurité
    // -------------------------------------------------------------------------

    public Securite securite() {
        Securite courant = securiteEnCache;
        if (courant != null && Instant.now().isBefore(chargementCache.plus(DUREE_CACHE))) {
            return courant;
        }
        Securite charge = chargerSecurite();
        securiteEnCache = charge;
        chargementCache = Instant.now();
        return charge;
    }

    public PolitiqueMotDePasse politiqueMotDePasse() {
        Securite securite = securite();
        return new PolitiqueMotDePasse(securite.longueurMinMotDePasse(), securite.exigerCaractereSpecial());
    }

    public boolean deuxFacteursObligatoire(RoleUtilisateur role) {
        return role != null && securite().roles2faObligatoire().contains(role.name());
    }

    public long dureeSessionMs() {
        return Duration.ofDays(securite().dureeSessionJours()).toMillis();
    }

    @Transactional
    public Securite modifierSecurite(Securite demande, String acteurEmail) {
        Set<String> rolesValides = Arrays.stream(RoleUtilisateur.values())
                .map(Enum::name)
                .collect(Collectors.toSet());
        Set<String> roles = new LinkedHashSet<>();
        for (String role : demande.roles2faObligatoire()) {
            String code = role.trim().toUpperCase(Locale.ROOT);
            if (!rolesValides.contains(code)) {
                throw new BusinessRuleException("ROLE_INVALIDE", "Rôle inconnu : " + role);
            }
            roles.add(code);
        }
        if (demande.longueurMinMotDePasse() < LONGUEUR_MDP_MIN || demande.longueurMinMotDePasse() > 64) {
            throw new BusinessRuleException("LONGUEUR_MDP_INVALIDE",
                    "La longueur minimale du mot de passe doit être comprise entre 10 et 64");
        }
        if (demande.dureeSessionJours() < 1 || demande.dureeSessionJours() > 30) {
            throw new BusinessRuleException("DUREE_SESSION_INVALIDE",
                    "La durée de session doit être comprise entre 1 et 30 jours");
        }

        Securite avant = chargerSecurite();
        Instant maintenant = Instant.now();
        ecrire(CLE_ROLES_2FA, String.join(",", roles), acteurEmail, maintenant);
        ecrire(CLE_LONGUEUR_MDP, Integer.toString(demande.longueurMinMotDePasse()), acteurEmail, maintenant);
        ecrire(CLE_SPECIAL_MDP, Boolean.toString(demande.exigerCaractereSpecial()), acteurEmail, maintenant);
        ecrire(CLE_DUREE_SESSION, Integer.toString(demande.dureeSessionJours()), acteurEmail, maintenant);

        Securite apres = new Securite(List.copyOf(roles), demande.longueurMinMotDePasse(),
                demande.exigerCaractereSpecial(), demande.dureeSessionJours());
        journalAuditService.enregistrer("MODIFIER_PARAMETRES_SECURITE", "ParametresPlateforme", null,
                acteurEmail, json(avant), json(apres), null);
        securiteEnCache = null;
        return apres;
    }

    @Transactional(readOnly = true)
    public ParametresPublics parametresPublics() {
        return new ParametresPublics(politiqueMotDePasse(), typesLaboratoireActifs(), identite());
    }

    // -------------------------------------------------------------------------
    // Identité de la plateforme
    // -------------------------------------------------------------------------

    public Identite identite() {
        Identite courant = identiteEnCache;
        if (courant != null && Instant.now().isBefore(chargementIdentite.plus(DUREE_CACHE))) {
            return courant;
        }
        Map<String, String> valeurs = toutesLesValeurs();
        Identite charge = new Identite(
                valeurOuDefaut(valeurs.get(CLE_NOM), NOM_PLATEFORME_DEFAUT),
                valeurs.getOrDefault(CLE_EMAIL_CONTACT, ""),
                valeurs.getOrDefault(CLE_TELEPHONE, ""),
                valeurs.getOrDefault(CLE_ADRESSE, ""),
                valeurOuDefaut(valeurs.get(CLE_EXPEDITEUR), EXPEDITEUR_DEFAUT));
        identiteEnCache = charge;
        chargementIdentite = Instant.now();
        return charge;
    }

    /** Nom affiché comme expéditeur des e-mails ; ne lève jamais d'exception. */
    public String nomExpediteur() {
        try {
            return identite().expediteurNom();
        } catch (RuntimeException ex) {
            log.warn("Nom d'expéditeur illisible, valeur par défaut utilisée : {}", ex.getMessage());
            return EXPEDITEUR_DEFAUT;
        }
    }

    @Transactional
    public Identite modifierIdentite(Identite demande, String acteurEmail) {
        Identite avant = identite();
        Instant maintenant = Instant.now();
        Identite apres = new Identite(
                demande.nom().trim(),
                texteOuVide(demande.emailContact()),
                texteOuVide(demande.telephone()),
                texteOuVide(demande.adresse()),
                demande.expediteurNom().trim());
        ecrire(CLE_NOM, apres.nom(), acteurEmail, maintenant);
        ecrire(CLE_EMAIL_CONTACT, apres.emailContact(), acteurEmail, maintenant);
        ecrire(CLE_TELEPHONE, apres.telephone(), acteurEmail, maintenant);
        ecrire(CLE_ADRESSE, apres.adresse(), acteurEmail, maintenant);
        ecrire(CLE_EXPEDITEUR, apres.expediteurNom(), acteurEmail, maintenant);
        journalAuditService.enregistrer("MODIFIER_IDENTITE_PLATEFORME", "ParametresPlateforme", null,
                acteurEmail, json(avant), json(apres), null);
        identiteEnCache = null;
        return apres;
    }

    // -------------------------------------------------------------------------
    // Commission de la plateforme
    // -------------------------------------------------------------------------

    /** Taux de commission de la plateforme (%), prélevé sur le montant de chaque facture. */
    public BigDecimal tauxCommission() {
        BigDecimal courant = commissionEnCache;
        if (courant != null && Instant.now().isBefore(chargementCommission.plus(DUREE_CACHE))) {
            return courant;
        }
        BigDecimal charge = COMMISSION_DEFAUT;
        // Lu dans le schéma central, quel que soit le tenant de la requête en cours.
        String brut = tenantExecutor.inCentral(() ->
                parametreRepository.findById(CLE_COMMISSION).map(ParametrePlateforme::getValeur).orElse(null));
        if (StringUtils.hasText(brut)) {
            try {
                charge = new BigDecimal(brut.trim()).setScale(2, RoundingMode.HALF_UP);
            } catch (NumberFormatException ex) {
                log.warn("Taux de commission illisible ({}), {} % appliqué", brut, COMMISSION_DEFAUT);
            }
        }
        commissionEnCache = charge;
        chargementCommission = Instant.now();
        return charge;
    }

    public Commission commission() {
        return new Commission(tauxCommission());
    }

    @Transactional
    public Commission modifierCommission(Commission demande, String acteurEmail) {
        BigDecimal avant = tauxCommission();
        BigDecimal taux = demande.tauxCommission().setScale(2, RoundingMode.HALF_UP);
        ecrire(CLE_COMMISSION, taux.toPlainString(), acteurEmail, Instant.now());
        journalAuditService.enregistrer("MODIFIER_COMMISSION_PLATEFORME", "ParametresPlateforme", null, acteurEmail,
                "{\"tauxCommission\":" + avant.toPlainString() + "}",
                "{\"tauxCommission\":" + taux.toPlainString() + "}", null);
        commissionEnCache = null;
        return new Commission(taux);
    }

    /** Commission sur un montant, arrondie au centime. */
    public static BigDecimal commissionSur(BigDecimal montant, BigDecimal taux) {
        if (montant == null || taux == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return montant.multiply(taux).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    /** Toutes les valeurs, lues dans le schéma central quel que soit le tenant de la requête. */
    private Map<String, String> toutesLesValeurs() {
        return tenantExecutor.inCentral(() -> parametreRepository.findAll().stream()
                .collect(Collectors.toMap(ParametrePlateforme::getCle, ParametrePlateforme::getValeur, (a, b) -> b)));
    }

    private static String valeurOuDefaut(String valeur, String defaut) {
        return StringUtils.hasText(valeur) ? valeur.trim() : defaut;
    }

    private static String texteOuVide(String valeur) {
        return valeur == null ? "" : valeur.trim();
    }

    private Securite chargerSecurite() {
        Map<String, String> valeurs = parametreRepository.findAll().stream()
                .collect(Collectors.toMap(ParametrePlateforme::getCle, ParametrePlateforme::getValeur, (a, b) -> b));
        List<String> roles = Arrays.stream(valeurs.getOrDefault(CLE_ROLES_2FA, "").split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
        int longueur = Math.max(LONGUEUR_MDP_MIN, entier(valeurs.get(CLE_LONGUEUR_MDP), LONGUEUR_MDP_MIN));
        boolean special = Boolean.parseBoolean(valeurs.getOrDefault(CLE_SPECIAL_MDP, "false"));
        int duree = entier(valeurs.get(CLE_DUREE_SESSION), DUREE_SESSION_DEFAUT);
        if (duree < 1 || duree > 30) {
            duree = DUREE_SESSION_DEFAUT;
        }
        return new Securite(roles, longueur, special, duree);
    }

    private void ecrire(String cle, String valeur, String acteurEmail, Instant date) {
        ParametrePlateforme parametre = parametreRepository.findById(cle)
                .orElseGet(() -> ParametrePlateforme.builder().cle(cle).build());
        parametre.setValeur(valeur);
        parametre.setDateModification(date);
        parametre.setModifiePar(acteurEmail);
        parametreRepository.save(parametre);
    }

    // -------------------------------------------------------------------------
    // Documents d'intégration
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<TypeDocumentIntegrationDto> listerTypesDocuments() {
        return typeDocumentRepository.findAllByOrderByOrdreAscLibelleAsc().stream()
                .map(TypeDocumentIntegrationDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TypeDocumentIntegration> typesDocumentsActifs() {
        return typeDocumentRepository.findByActifTrueOrderByOrdreAscLibelleAsc();
    }

    @Transactional(readOnly = true)
    public Map<String, TypeDocumentIntegration> typesDocumentsParCode() {
        return typeDocumentRepository.findAll().stream()
                .collect(Collectors.toMap(TypeDocumentIntegration::getCode, Function.identity()));
    }

    @Transactional
    public TypeDocumentIntegrationDto creerTypeDocument(EnregistrerTypeDocument demande, String acteurEmail) {
        String code = codeDepuis(demande.libelle());
        if (typeDocumentRepository.existsById(code)) {
            throw new BusinessRuleException("TYPE_DOCUMENT_EXISTANT",
                    "Un document portant un libellé équivalent existe déjà");
        }
        TypeDocumentIntegration type = typeDocumentRepository.save(TypeDocumentIntegration.builder()
                .code(code)
                .libelle(demande.libelle().trim())
                .raison(texte(demande.raison()))
                .obligatoire(demande.obligatoire())
                .actif(demande.actif())
                .ordre(demande.ordre())
                .dateCreation(Instant.now())
                .build());
        TypeDocumentIntegrationDto dto = TypeDocumentIntegrationDto.from(type);
        journalAuditService.enregistrer("CREER_TYPE_DOCUMENT_INTEGRATION", "TypeDocumentIntegration", null,
                acteurEmail, null, json(dto), null);
        return dto;
    }

    @Transactional
    public TypeDocumentIntegrationDto modifierTypeDocument(String code, EnregistrerTypeDocument demande,
                                                           String acteurEmail) {
        TypeDocumentIntegration type = typeDocumentRepository.findById(code)
                .orElseThrow(() -> new ResourceNotFoundException("TypeDocumentIntegration", "code", code));
        TypeDocumentIntegrationDto avant = TypeDocumentIntegrationDto.from(type);
        type.setLibelle(demande.libelle().trim());
        type.setRaison(texte(demande.raison()));
        type.setObligatoire(demande.obligatoire());
        type.setActif(demande.actif());
        type.setOrdre(demande.ordre());
        TypeDocumentIntegrationDto apres = TypeDocumentIntegrationDto.from(typeDocumentRepository.save(type));
        journalAuditService.enregistrer("MODIFIER_TYPE_DOCUMENT_INTEGRATION", "TypeDocumentIntegration", null,
                acteurEmail, json(avant), json(apres), null);
        return apres;
    }

    // -------------------------------------------------------------------------
    // Types de laboratoire
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<TypeLaboratoire> listerTypesLaboratoire() {
        return typeLaboratoireRepository.findAllByOrderByOrdreAscLibelleAsc().stream()
                .map(ParametrePlateformeService::versDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> typesLaboratoireActifs() {
        return typeLaboratoireRepository.findByActifTrueOrderByOrdreAscLibelleAsc().stream()
                .map(ReferentielTypeLaboratoire::getLibelle)
                .toList();
    }

    @Transactional
    public TypeLaboratoire creerTypeLaboratoire(CreerTypeLaboratoire demande, String acteurEmail) {
        String libelle = demande.libelle().trim();
        if (libelle.contains(",")) {
            throw new BusinessRuleException("TYPE_LABORATOIRE_INVALIDE",
                    "Le libellé ne peut pas contenir de virgule");
        }
        if (typeLaboratoireRepository.existsByLibelleIgnoreCase(libelle)) {
            throw new BusinessRuleException("TYPE_LABORATOIRE_EXISTANT", "Ce type de laboratoire existe déjà");
        }
        TypeLaboratoire dto = versDto(typeLaboratoireRepository.save(ReferentielTypeLaboratoire.builder()
                .libelle(libelle)
                .actif(true)
                .ordre(demande.ordre())
                .build()));
        journalAuditService.enregistrer("CREER_TYPE_LABORATOIRE", "ReferentielTypeLaboratoire", dto.id(),
                acteurEmail, null, json(dto), null);
        return dto;
    }

    @Transactional
    public TypeLaboratoire modifierTypeLaboratoire(Long id, ModifierTypeLaboratoire demande, String acteurEmail) {
        ReferentielTypeLaboratoire type = typeLaboratoireRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ReferentielTypeLaboratoire", "id", id));
        TypeLaboratoire avant = versDto(type);
        type.setActif(demande.actif());
        type.setOrdre(demande.ordre());
        TypeLaboratoire apres = versDto(typeLaboratoireRepository.save(type));
        journalAuditService.enregistrer("MODIFIER_TYPE_LABORATOIRE", "ReferentielTypeLaboratoire", id,
                acteurEmail, json(avant), json(apres), null);
        return apres;
    }

    // -------------------------------------------------------------------------
    // Utilitaires
    // -------------------------------------------------------------------------

    static String codeDepuis(String libelle) {
        String sansAccents = Normalizer.normalize(libelle == null ? "" : libelle, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        String code = sansAccents.toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (code.isEmpty()) {
            throw new BusinessRuleException("LIBELLE_INVALIDE", "Le libellé doit contenir des lettres ou des chiffres");
        }
        return code.length() > 80 ? code.substring(0, 80) : code;
    }

    private static TypeLaboratoire versDto(ReferentielTypeLaboratoire type) {
        return new TypeLaboratoire(type.getId(), type.getLibelle(), type.isActif(), type.getOrdre());
    }

    private static int entier(String valeur, int defaut) {
        try {
            return valeur == null ? defaut : Integer.parseInt(valeur.trim());
        } catch (NumberFormatException ex) {
            return defaut;
        }
    }

    private static String texte(String valeur) {
        return StringUtils.hasText(valeur) ? valeur.trim() : null;
    }

    private String json(Object valeur) {
        try {
            return objectMapper.writeValueAsString(valeur);
        } catch (JsonProcessingException ex) {
            log.warn("Valeur d'audit non sérialisable : {}", ex.getMessage());
            return null;
        }
    }
}
