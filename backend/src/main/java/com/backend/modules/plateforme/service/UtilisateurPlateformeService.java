package com.backend.modules.plateforme.service;

import com.backend.common.audit.JournalAuditService;
import com.backend.common.exception.BusinessRuleException;
import com.backend.common.exception.ResourceNotFoundException;
import com.backend.modules.auth.repository.RefreshTokenRepository;
import com.backend.modules.plateforme.dto.CompteUtilisateurDto;
import com.backend.modules.plateforme.dto.FichierGenere;
import com.backend.modules.plateforme.dto.ModifierIdentiteCompteRequest;
import com.backend.modules.plateforme.dto.SessionCompteDto;
import com.backend.common.util.ExportTableau;
import com.backend.modules.plateforme.entity.Laboratoire;
import com.backend.modules.utilisateur.entity.RoleUtilisateur;
import com.backend.modules.utilisateur.entity.Utilisateur;
import com.backend.modules.utilisateur.repository.UtilisateurPermissionRepository;
import com.backend.modules.utilisateur.repository.UtilisateurRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UtilisateurPlateformeService {

    static final int TAILLE_MAX = 100;
    static final int EXPORT_MAX = 5000;
    private static final Set<String> TRIS = Set.of("nom", "prenom", "email", "derniereConnexion", "role");
    private static final List<String> COLONNES = List.of(
            "Nom", "Prénom", "E-mail", "Rôle", "Laboratoire", "Statut", "Dernière connexion");
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("Africa/Casablanca"));

    private final UtilisateurRepository utilisateurRepository;
    private final UtilisateurPermissionRepository utilisateurPermissionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JournalAuditService journalAuditService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Page<CompteUtilisateurDto> lister(String recherche, String role, Long laboratoireId, String statut,
                                             int page, int size, String tri, String ordre) {
        Page<Utilisateur> trouves = utilisateurRepository.rechercherComptesPlateforme(
                roleAdministrateur(role),
                laboratoireId,
                actifFiltre(statut),
                like(recherche),
                page(page, size, tri, ordre));
        List<Long> ids = trouves.getContent().stream().map(Utilisateur::getId).toList();
        Set<Long> personnalises = ids.isEmpty()
                ? Set.of()
                : new HashSet<>(utilisateurPermissionRepository.identifiantsPersonnalises(ids));
        return trouves.map(compte -> versDto(compte, personnalises.contains(compte.getId())));
    }

    @Transactional(readOnly = true)
    public FichierGenere exporter(String format, String recherche, String role, Long laboratoireId, String statut,
                                  String tri, String ordre) {
        Page<Utilisateur> page = utilisateurRepository.rechercherComptesPlateforme(
                roleAdministrateur(role),
                laboratoireId,
                actifFiltre(statut),
                like(recherche),
                PageRequest.of(0, EXPORT_MAX, tri(tri, ordre)));
        if (page.getTotalElements() > EXPORT_MAX) {
            throw new BusinessRuleException("EXPORT_TROP_LARGE",
                    "Plus de " + EXPORT_MAX + " lignes correspondent. Affinez les filtres avant d'exporter.");
        }
        List<List<String>> lignes = page.getContent().stream().map(this::valeurs).toList();
        return ExportTableau.generer(format, "administrateurs", "Administrateurs de laboratoire",
                COLONNES, lignes);
    }

    /**
     * Efface le secret TOTP : à la prochaine connexion, l'administrateur reconfigure
     * sa 2FA (ou se connecte sans si elle n'est pas obligatoire pour son rôle).
     * Ses sessions ouvertes sont révoquées.
     */
    @Transactional
    public CompteUtilisateurDto reinitialiserDeuxFacteurs(Long id, Long acteurId, String acteurEmail, String motif) {
        Utilisateur compte = charger(id);
        refuserComptePlateforme(compte, acteurId);
        if (!compte.isDoubleAuthentification() && !StringUtils.hasText(compte.getSecret2fa())) {
            throw new BusinessRuleException("2FA_NON_CONFIGUREE",
                    "La double authentification n'est pas configurée sur ce compte");
        }
        compte.setDoubleAuthentification(false);
        compte.setSecret2fa(null);
        utilisateurRepository.save(compte);
        int sessions = refreshTokenRepository.revoquerTousParUtilisateur(compte.getId());
        journalAuditService.enregistrer(
                "REINITIALISER_2FA", "Utilisateur", compte.getId(), acteurEmail,
                "{\"doubleAuthentification\":true}", "{\"doubleAuthentification\":false,\"sessionsRevoquees\":" + sessions + "}",
                motifOuNull(motif), idLaboratoire(compte));
        log.info("2FA réinitialisée et {} session(s) révoquée(s) : userId={}", sessions, compte.getId());
        return versDto(compte);
    }

    @Transactional(readOnly = true)
    public List<SessionCompteDto> sessions(Long id) {
        Utilisateur compte = charger(id);
        return refreshTokenRepository.listerActifs(compte.getId(), Instant.now()).stream()
                .map(jeton -> new SessionCompteDto(jeton.getId(), jeton.getUserAgent(),
                        jeton.getCreatedAt(), jeton.getDateExpiration()))
                .toList();
    }

    /**
     * Révoque toutes les sessions. Le jeton d'accès déjà émis reste valable
     * jusqu'à son expiration (15 minutes par défaut).
     */
    @Transactional
    public int revoquerSessions(Long id, Long acteurId, String acteurEmail, String motif) {
        Utilisateur compte = charger(id);
        refuserComptePlateforme(compte, acteurId);
        int sessions = refreshTokenRepository.revoquerTousParUtilisateur(compte.getId());
        journalAuditService.enregistrer(
                "REVOQUER_SESSIONS", "Utilisateur", compte.getId(), acteurEmail,
                null, "{\"sessionsRevoquees\":" + sessions + "}", motifOuNull(motif), idLaboratoire(compte));
        return sessions;
    }

    @Transactional(readOnly = true)
    public CompteUtilisateurDto detail(Long id) {
        return versDto(charger(id));
    }

    @Transactional
    public CompteUtilisateurDto modifierIdentite(Long id, ModifierIdentiteCompteRequest request,
                                                  Long acteurId, String acteurEmail) {
        Utilisateur compte = charger(id);
        refuserComptePlateforme(compte, acteurId);
        String avant = snapshot(compte);
        compte.setNom(request.getNom().trim());
        compte.setPrenom(texte(request.getPrenom()));
        compte.setTelephone(texte(request.getTelephone()));
        compte.setFonction(texte(request.getFonction()));
        utilisateurRepository.save(compte);
        journalAuditService.enregistrer(
                "MODIFIER_IDENTITE_COMPTE", "Utilisateur", compte.getId(), acteurEmail,
                avant, snapshot(compte), null, idLaboratoire(compte));
        return versDto(compte);
    }

    @Transactional
    public CompteUtilisateurDto desactiver(Long id, Long acteurId, String acteurEmail, String motif) {
        Utilisateur compte = charger(id);
        refuserComptePlateforme(compte, acteurId);
        if (!compte.isActif()) {
            throw new BusinessRuleException("COMPTE_DEJA_INACTIF", "Ce compte est déjà désactivé");
        }
        String avant = snapshot(compte);
        compte.setActif(false);
        utilisateurRepository.save(compte);
        refreshTokenRepository.revoquerTousParUtilisateur(compte.getId());
        journalAuditService.enregistrer(
                "DESACTIVER_UTILISATEUR", "Utilisateur", compte.getId(), acteurEmail,
                avant, snapshot(compte), motifOuNull(motif), idLaboratoire(compte));
        log.info("Compte désactivé et sessions révoquées : userId={}", compte.getId());
        return versDto(compte);
    }

    @Transactional
    public CompteUtilisateurDto reactiver(Long id, Long acteurId, String acteurEmail, String motif) {
        Utilisateur compte = charger(id);
        refuserComptePlateforme(compte, acteurId);
        if (compte.isActif()) {
            throw new BusinessRuleException("COMPTE_DEJA_ACTIF", "Ce compte est déjà actif");
        }
        if (compte.getRole() != null
                && RoleUtilisateur.ADMINISTRATEUR.name().equals(compte.getRole().getCode())) {
            if (compte.getLaboratoire() == null) {
                throw new BusinessRuleException("ADMINISTRATEUR_SANS_LABORATOIRE",
                        "Cet administrateur n'est rattaché à aucun laboratoire");
            }
            if (utilisateurRepository.compterAdministrateursActifs(compte.getLaboratoire().getId()) > 0) {
                throw new BusinessRuleException("ADMINISTRATEUR_ACTIF_EXISTANT",
                        "Ce laboratoire a déjà un administrateur actif");
            }
        }
        String avant = snapshot(compte);
        compte.setActif(true);
        utilisateurRepository.save(compte);
        journalAuditService.enregistrer(
                "REACTIVER_UTILISATEUR", "Utilisateur", compte.getId(), acteurEmail,
                avant, snapshot(compte), motifOuNull(motif), idLaboratoire(compte));
        return versDto(compte);
    }

    private Utilisateur charger(Long id) {
        Utilisateur compte = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", id));
        exigerAdministrateurLaboratoire(compte);
        return compte;
    }

    private static void exigerAdministrateurLaboratoire(Utilisateur compte) {
        if (compte.getRole() == null
                || !RoleUtilisateur.ADMINISTRATEUR.name().equals(compte.getRole().getCode())) {
            throw new BusinessRuleException("COMPTE_HORS_PERIMETRE",
                    "Cet écran ne gère que les administrateurs de laboratoire");
        }
    }

    private static void refuserComptePlateforme(Utilisateur compte, Long acteurId) {
        boolean superAdmin = compte.getRole() != null
                && RoleUtilisateur.SUPER_ADMINISTRATEUR.name().equals(compte.getRole().getCode());
        if (superAdmin || (acteurId != null && acteurId.equals(compte.getId()))) {
            throw new BusinessRuleException("COMPTE_PLATEFORME",
                    "Compte plateforme : ce compte ne peut pas être modifié");
        }
    }

    private CompteUtilisateurDto versDto(Utilisateur compte) {
        return versDto(compte, false);
    }

    private CompteUtilisateurDto versDto(Utilisateur compte, boolean personnalise) {
        Laboratoire laboratoire = compte.getLaboratoire();
        return CompteUtilisateurDto.builder()
                .id(compte.getId())
                .nom(compte.getNom())
                .prenom(compte.getPrenom())
                .email(compte.getEmail())
                .telephone(compte.getTelephone())
                .fonction(compte.getFonction())
                .role(compte.getRole() == null ? null : compte.getRole().getCode())
                .laboratoireId(laboratoire == null ? null : laboratoire.getId())
                .laboratoireNom(laboratoire == null ? null : laboratoire.getRaisonSociale())
                .statut(compte.isActif() ? "ACTIF" : "INACTIF")
                .derniereConnexion(compte.getDerniereConnexion())
                .personnalise(personnalise)
                .deuxFacteursActive(compte.isDoubleAuthentification() && StringUtils.hasText(compte.getSecret2fa()))
                .build();
    }

    private String snapshot(Utilisateur compte) {
        Map<String, Object> valeurs = new LinkedHashMap<>();
        valeurs.put("nom", compte.getNom());
        valeurs.put("prenom", compte.getPrenom());
        valeurs.put("telephone", compte.getTelephone());
        valeurs.put("fonction", compte.getFonction());
        valeurs.put("email", compte.getEmail());
        valeurs.put("role", compte.getRole() == null ? null : compte.getRole().getCode());
        valeurs.put("actif", compte.isActif());
        valeurs.put("laboratoireId", idLaboratoire(compte));
        try {
            return objectMapper.writeValueAsString(valeurs);
        } catch (JsonProcessingException ex) {
            log.error("Instantané utilisateur non sérialisable : {}", ex.getMessage());
            return "{}";
        }
    }

    private PageRequest page(int page, int size, String tri, String ordre) {
        int pageSafe = Math.max(page, 0);
        int sizeSafe = Math.min(Math.max(size, 1), TAILLE_MAX);
        return PageRequest.of(pageSafe, sizeSafe, tri(tri, ordre));
    }

    private static Sort tri(String tri, String ordre) {
        String champ = TRIS.contains(tri) ? ("role".equals(tri) ? "role.code" : tri) : "nom";
        Sort.Direction direction = "desc".equalsIgnoreCase(ordre) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(direction, champ);
    }

    private static String roleAdministrateur(String role) {
        if (StringUtils.hasText(role)
                && !RoleUtilisateur.ADMINISTRATEUR.name().equals(role.trim().toUpperCase(Locale.ROOT))) {
            throw new BusinessRuleException("ROLE_FILTRE_INVALIDE",
                    "Cet écran ne gère que les administrateurs de laboratoire");
        }
        return RoleUtilisateur.ADMINISTRATEUR.name();
    }

    private static Boolean actifFiltre(String statut) {
        if (!StringUtils.hasText(statut)) {
            return null;
        }
        return switch (statut.trim().toUpperCase(Locale.ROOT)) {
            case "ACTIF" -> Boolean.TRUE;
            case "INACTIF" -> Boolean.FALSE;
            default -> throw new BusinessRuleException("STATUT_FILTRE_INVALIDE",
                    "Le statut doit être ACTIF ou INACTIF");
        };
    }

    private List<String> valeurs(Utilisateur compte) {
        Laboratoire laboratoire = compte.getLaboratoire();
        return List.of(
                texte(compte.getNom()),
                texte(compte.getPrenom()),
                texte(compte.getEmail()),
                compte.getRole() == null ? "" : compte.getRole().getCode(),
                laboratoire == null ? "" : texte(laboratoire.getRaisonSociale()),
                compte.isActif() ? "Actif" : "Inactif",
                compte.getDerniereConnexion() == null ? "" : DATE.format(compte.getDerniereConnexion()));
    }

    private static Long idLaboratoire(Utilisateur compte) {
        return compte.getLaboratoire() == null ? null : compte.getLaboratoire().getId();
    }

    private static String texte(String valeur) {
        return valeur == null ? "" : valeur.trim();
    }

    private static String motifOuNull(String valeur) {
        return StringUtils.hasText(valeur) ? valeur.trim() : null;
    }

    private static String like(String valeur) {
        if (!StringUtils.hasText(valeur)) {
            return null;
        }
        return valeur.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
