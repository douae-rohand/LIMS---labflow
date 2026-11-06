package com.backend.modules.plateforme.service;

import com.backend.common.audit.JournalAuditService;
import com.backend.common.exception.BusinessRuleException;
import com.backend.common.exception.ResourceNotFoundException;
import com.backend.modules.auth.security.AutorisationService;
import com.backend.modules.plateforme.dto.LignePermissionDto;
import com.backend.modules.plateforme.dto.PermissionsCompteDto;
import com.backend.modules.plateforme.dto.RemplacerEcartsRequest;
import com.backend.modules.utilisateur.entity.EffetPermission;
import com.backend.modules.utilisateur.entity.Permission;
import com.backend.modules.utilisateur.entity.RoleUtilisateur;
import com.backend.modules.utilisateur.entity.Utilisateur;
import com.backend.modules.utilisateur.entity.UtilisateurPermission;
import com.backend.modules.utilisateur.repository.UtilisateurPermissionRepository;
import com.backend.modules.utilisateur.repository.UtilisateurRepository;
import com.backend.modules.auth.security.UtilisateurPrincipal;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PermissionCompteService {

    static final List<String> ACTIONS_NON_DELEGABLES = List.of(
            "Acceptation des demandes",
            "Refus des demandes",
            "Décision de validation",
            "Statut des rapports");

    private final UtilisateurRepository utilisateurRepository;
    private final UtilisateurPermissionRepository utilisateurPermissionRepository;
    private final AutorisationService autorisationService;
    private final JournalAuditService journalAuditService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public PermissionsCompteDto lire(Long id) {
        Utilisateur compte = charger(id);
        return assembler(compte);
    }

    @Transactional(readOnly = true)
    public PermissionsCompteDto lirePourEmploye(Utilisateur compte) {
        return assembler(compte);
    }

    @Transactional
    public PermissionsCompteDto remplacer(Long id, RemplacerEcartsRequest request,
                                          Long acteurId, String acteurEmail) {
        Utilisateur compte = charger(id);
        refuserPersonnalisation(compte, acteurId);
        Map<String, EffetPermission> demandes = normaliser(request);
        verifier(compte, demandes);
        String avant = json(ecartsActuels(id));
        utilisateurPermissionRepository.deleteByUtilisateurId(id);
        utilisateurPermissionRepository.flush();
        Map<String, Long> ids = autorisationService.idsParCode();
        List<UtilisateurPermission> lignes = new ArrayList<>();
        for (Map.Entry<String, EffetPermission> ecart : demandes.entrySet()) {
            lignes.add(UtilisateurPermission.builder()
                    .utilisateurId(id)
                    .permissionId(ids.get(ecart.getKey()))
                    .effet(ecart.getValue())
                    .build());
        }
        utilisateurPermissionRepository.saveAll(lignes);
        journalAuditService.enregistrer(
                "REMPLACER_PERMISSIONS", "Utilisateur", id, acteurEmail,
                avant, json(demandes), null, laboratoireId(compte));
        return assembler(compte);
    }

    @Transactional
    public PermissionsCompteDto retablir(Long id, Long acteurId, String acteurEmail) {
        Utilisateur compte = charger(id);
        refuserPersonnalisation(compte, acteurId);
        String avant = json(ecartsActuels(id));
        utilisateurPermissionRepository.deleteByUtilisateurId(id);
        journalAuditService.enregistrer(
                "RETABLIR_PERMISSIONS", "Utilisateur", id, acteurEmail,
                avant, "{}", null, laboratoireId(compte));
        return assembler(compte);
    }

    @Transactional
    public PermissionsCompteDto remplacerPourEmploye(
            Utilisateur compte,
            RemplacerEcartsRequest request,
            UtilisateurPrincipal acteur) {
        refuserAutoModification(compte, acteur.getId());
        Map<String, EffetPermission> demandes = normaliser(request);
        Map<String, Permission> catalogue = permissionsParCode();
        Set<String> permissionsActeur = acteur.getPermissions() == null
                ? Set.of()
                : acteur.getPermissions();
        for (Map.Entry<String, EffetPermission> demande : demandes.entrySet()) {
            Permission permission = catalogue.get(demande.getKey());
            if (!permission.isDeleguable()) {
                throw new BusinessRuleException("PERMISSION_NON_DELEGABLE",
                        "La permission « " + permission.getLibelle() + " » ne peut pas être personnalisée.");
            }
            if (demande.getValue() == EffetPermission.AJOUT
                    && !permissionsActeur.contains(demande.getKey())) {
                throw new BusinessRuleException("PERMISSION_NON_AUTORISEE",
                        "Vous ne pouvez accorder que les permissions que vous possédez.");
            }
        }

        Map<String, EffetPermission> avant = ecartsActuels(compte.getId());
        Map<String, EffetPermission> apres = new LinkedHashMap<>();
        avant.forEach((code, effet) -> {
            Permission permission = catalogue.get(code);
            if (permission != null && !permission.isDeleguable()) {
                apres.put(code, effet);
            }
        });
        apres.putAll(demandes);
        verifierEcarts(compte, apres);
        remplacerPermissionsDelegables(compte.getId(), demandes, catalogue);
        if (!avant.equals(apres)) {
            journalAuditService.enregistrer(
                    "MODIFICATION_PERMISSIONS", "Utilisateur", compte.getId(), acteur.getEmail(),
                    json(ecartsCodes(avant)), json(ecartsCodes(apres)), null, laboratoireId(compte));
        }
        return assembler(compte);
    }

    @Transactional
    public PermissionsCompteDto retablirPourEmploye(Utilisateur compte, UtilisateurPrincipal acteur) {
        refuserAutoModification(compte, acteur.getId());
        Map<String, EffetPermission> avant = ecartsActuels(compte.getId());
        Map<String, Permission> catalogue = permissionsParCode();
        Map<String, EffetPermission> apres = new LinkedHashMap<>(avant);
        apres.entrySet().removeIf(ecart -> {
            Permission permission = catalogue.get(ecart.getKey());
            return permission != null && permission.isDeleguable();
        });
        verifierEcarts(compte, apres);
        supprimerPermissionsDelegables(compte.getId(), catalogue);
        if (!avant.equals(apres)) {
            journalAuditService.enregistrer(
                    "MODIFICATION_PERMISSIONS", "Utilisateur", compte.getId(), acteur.getEmail(),
                    json(ecartsCodes(avant)), json(ecartsCodes(apres)), null, laboratoireId(compte));
        }
        return assembler(compte);
    }

    private PermissionsCompteDto assembler(Utilisateur compte) {
        String role = compte.getRole() == null ? null : compte.getRole().getCode();
        boolean personnalisable = role != null
                && autorisationService.personnalisable(RoleUtilisateur.valueOf(role));
        Set<String> defauts = personnalisable ? autorisationService.defauts(role) : Set.of();
        Map<String, EffetPermission> ecarts = personnalisable ? ecartsActuels(compte.getId()) : Map.of();
        List<LignePermissionDto> lignes = new ArrayList<>();
        for (Permission permission : autorisationService.catalogue()) {
            boolean parDefaut = defauts.contains(permission.getCode());
            EffetPermission ecart = ecarts.get(permission.getCode());
            boolean effective = parDefaut;
            if (ecart == EffetPermission.AJOUT) {
                effective = true;
            } else if (ecart == EffetPermission.RETRAIT) {
                effective = false;
            }
            lignes.add(LignePermissionDto.builder()
                    .code(permission.getCode())
                    .module(permission.getModule())
                    .action(permission.getAction())
                    .libelle(permission.getLibelle())
                    .parDefaut(parDefaut)
                    .deleguable(permission.isDeleguable())
                    .ecart(ecart == null ? null : ecart.name())
                    .effective(personnalisable && effective)
                    .build());
        }
        return PermissionsCompteDto.builder()
                .personnalisable(personnalisable)
                .personnalisees(personnalisable && !ecarts.isEmpty())
                .permissions(lignes)
                .actionsNonDelegables(ACTIONS_NON_DELEGABLES)
                .build();
    }

    private void verifier(Utilisateur compte, Map<String, EffetPermission> demandes) {
        String role = compte.getRole().getCode();
        Set<String> effectives = new HashSet<>(autorisationService.defauts(role));
        for (Map.Entry<String, EffetPermission> ecart : demandes.entrySet()) {
            if (ecart.getValue() == EffetPermission.AJOUT) {
                effectives.add(ecart.getKey());
            } else {
                effectives.remove(ecart.getKey());
            }
        }
        autorisationService.verifierCoherence(effectives);
    }

    private void verifierEcarts(Utilisateur compte, Map<String, EffetPermission> ecarts) {
        Set<String> effectives = new HashSet<>(autorisationService.defauts(compte.getRole().getCode()));
        for (Map.Entry<String, EffetPermission> ecart : ecarts.entrySet()) {
            if (ecart.getValue() == EffetPermission.AJOUT) {
                effectives.add(ecart.getKey());
            } else {
                effectives.remove(ecart.getKey());
            }
        }
        autorisationService.verifierCoherence(effectives);
    }

    private Map<String, EffetPermission> normaliser(RemplacerEcartsRequest request) {
        Map<String, Long> ids = autorisationService.idsParCode();
        Map<String, EffetPermission> demandes = new LinkedHashMap<>();
        for (RemplacerEcartsRequest.Ecart ecart : request.getEcarts()) {
            String code = ecart.getCode().trim();
            if (!ids.containsKey(code)) {
                throw new BusinessRuleException("PERMISSION_INCONNUE",
                        "Permission inconnue : " + code + ".");
            }
            if (demandes.put(code, ecart.getEffet()) != null) {
                throw new BusinessRuleException("PERMISSION_DUPLIQUEE",
                        "La permission « " + autorisationService.libelle(code) + " » est indiquée deux fois.");
            }
        }
        return demandes;
    }

    private void remplacerPermissionsDelegables(
            Long utilisateurId,
            Map<String, EffetPermission> demandes,
            Map<String, Permission> catalogue) {
        supprimerPermissionsDelegables(utilisateurId, catalogue);
        Map<String, Long> ids = autorisationService.idsParCode();
        List<UtilisateurPermission> lignes = new ArrayList<>();
        for (Map.Entry<String, EffetPermission> ecart : demandes.entrySet()) {
            lignes.add(UtilisateurPermission.builder()
                    .utilisateurId(utilisateurId)
                    .permissionId(ids.get(ecart.getKey()))
                    .effet(ecart.getValue())
                    .build());
        }
        utilisateurPermissionRepository.saveAll(lignes);
    }

    private void supprimerPermissionsDelegables(Long utilisateurId, Map<String, Permission> catalogue) {
        Map<String, Long> ids = autorisationService.idsParCode();
        List<Long> permissionIds = catalogue.values().stream()
                .filter(Permission::isDeleguable)
                .map(permission -> ids.get(permission.getCode()))
                .toList();
        if (!permissionIds.isEmpty()) {
            utilisateurPermissionRepository.deleteByUtilisateurIdAndPermissionIdIn(utilisateurId, permissionIds);
            utilisateurPermissionRepository.flush();
        }
    }

    private Map<String, Permission> permissionsParCode() {
        Map<String, Permission> permissions = new LinkedHashMap<>();
        for (Permission permission : autorisationService.catalogue()) {
            permissions.put(permission.getCode(), permission);
        }
        return permissions;
    }

    private Map<String, List<String>> ecartsCodes(Map<String, EffetPermission> ecarts) {
        List<String> ajoutees = ecarts.entrySet().stream()
                .filter(ecart -> ecart.getValue() == EffetPermission.AJOUT)
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
        List<String> retirees = ecarts.entrySet().stream()
                .filter(ecart -> ecart.getValue() == EffetPermission.RETRAIT)
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
        return Map.of("ajoutees", ajoutees, "retirees", retirees);
    }

    private void refuserAutoModification(Utilisateur compte, Long acteurId) {
        if (acteurId != null && acteurId.equals(compte.getId())) {
            throw new ResourceNotFoundException("Utilisateur", "id", compte.getId());
        }
    }

    private Map<String, EffetPermission> ecartsActuels(Long utilisateurId) {
        Map<String, EffetPermission> ecarts = new LinkedHashMap<>();
        for (Object[] ligne : utilisateurPermissionRepository.listerEcarts(utilisateurId)) {
            ecarts.put((String) ligne[1], (EffetPermission) ligne[0]);
        }
        return ecarts;
    }

    private void refuserPersonnalisation(Utilisateur compte, Long acteurId) {
        boolean superAdmin = compte.getRole() != null
                && RoleUtilisateur.SUPER_ADMINISTRATEUR.name().equals(compte.getRole().getCode());
        boolean client = compte.getRole() != null
                && RoleUtilisateur.CLIENT.name().equals(compte.getRole().getCode());
        if (superAdmin || (acteurId != null && acteurId.equals(compte.getId()))) {
            throw new BusinessRuleException("COMPTE_PLATEFORME",
                    "Compte plateforme : ce compte ne peut pas être modifié");
        }
        if (client) {
            throw new BusinessRuleException("CLIENT_NON_PERSONNALISABLE",
                    "Les permissions d'un client ne sont pas personnalisables.");
        }
    }

    private Utilisateur charger(Long id) {
        return utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", id));
    }

    private static Long laboratoireId(Utilisateur compte) {
        return compte.getLaboratoire() == null ? null : compte.getLaboratoire().getId();
    }

    private String json(Object valeur) {
        try {
            return objectMapper.writeValueAsString(valeur);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Impossible de sérialiser les permissions pour l’audit.", ex);
        }
    }
}
