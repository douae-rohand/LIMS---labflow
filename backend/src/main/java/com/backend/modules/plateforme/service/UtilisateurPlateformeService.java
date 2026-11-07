package com.backend.modules.plateforme.service;

import com.backend.common.audit.JournalAuditService;
import com.backend.common.exception.BusinessRuleException;
import com.backend.common.exception.ResourceNotFoundException;
import com.backend.modules.auth.repository.RefreshTokenRepository;
import com.backend.modules.plateforme.dto.CompteUtilisateurDto;
import com.backend.modules.plateforme.dto.FichierGenere;
import com.backend.modules.plateforme.dto.ModifierIdentiteCompteRequest;
import com.backend.modules.plateforme.entity.Laboratoire;
import com.backend.modules.utilisateur.entity.RoleUtilisateur;
import com.backend.modules.utilisateur.entity.Utilisateur;
import com.backend.modules.utilisateur.repository.UtilisateurPermissionRepository;
import com.backend.modules.utilisateur.repository.UtilisateurRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
        List<Utilisateur> comptes = page.getContent();
        String jour = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.of("Africa/Casablanca")).format(Instant.now());
        return switch (normaliserFormat(format)) {
            case "xlsx" -> new FichierGenere(
                    "administrateurs-" + jour + ".xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    excel(comptes));
            case "csv" -> new FichierGenere(
                    "administrateurs-" + jour + ".csv",
                    "text/csv; charset=UTF-8",
                    csv(comptes));
            default -> throw new BusinessRuleException("FORMAT_EXPORT_INVALIDE",
                    "Le format d'exportation doit être Excel ou CSV");
        };
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

    private static String normaliserFormat(String format) {
        if (format == null) {
            return "";
        }
        return switch (format.trim().toLowerCase(Locale.ROOT)) {
            case "xlsx", "excel" -> "xlsx";
            case "csv" -> "csv";
            default -> "";
        };
    }

    private byte[] csv(List<Utilisateur> comptes) {
        StringBuilder builder = new StringBuilder();
        builder.append('\uFEFF');
        builder.append(String.join(";", COLONNES)).append("\r\n");
        for (Utilisateur compte : comptes) {
            List<String> cellules = valeurs(compte);
            for (int i = 0; i < cellules.size(); i++) {
                if (i > 0) {
                    builder.append(';');
                }
                builder.append(cellule(cellules.get(i)));
            }
            builder.append("\r\n");
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] excel(List<Utilisateur> comptes) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            Sheet feuille = workbook.createSheet("Utilisateurs");
            Row entete = feuille.createRow(0);
            for (int i = 0; i < COLONNES.size(); i++) {
                entete.createCell(i).setCellValue(COLONNES.get(i));
            }
            for (int ligne = 0; ligne < comptes.size(); ligne++) {
                Row row = feuille.createRow(ligne + 1);
                List<String> cellules = valeurs(comptes.get(ligne));
                for (int i = 0; i < cellules.size(); i++) {
                    row.createCell(i).setCellValue(protege(cellules.get(i)));
                }
            }
            workbook.write(sortie);
            return sortie.toByteArray();
        } catch (IOException ex) {
            throw new BusinessRuleException("EXPORT_ECHOUE", "Le fichier Excel n'a pas pu être généré");
        }
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

    private static String cellule(String valeur) {
        return "\"" + protege(valeur).replace("\"", "\"\"") + "\"";
    }

    private static String protege(String valeur) {
        String texte = valeur == null ? "" : valeur;
        int index = 0;
        while (index < texte.length()) {
            char caractere = texte.charAt(index);
            if (caractere != ' ' && caractere != '\t' && caractere != '\r') {
                break;
            }
            index++;
        }
        if (index < texte.length() && "=+-@".indexOf(texte.charAt(index)) >= 0) {
            return "'" + texte;
        }
        return texte;
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
