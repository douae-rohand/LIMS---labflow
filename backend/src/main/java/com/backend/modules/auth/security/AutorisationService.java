package com.backend.modules.auth.security;

import com.backend.common.exception.BusinessRuleException;
import com.backend.modules.utilisateur.entity.EffetPermission;
import com.backend.modules.utilisateur.entity.Permission;
import com.backend.modules.utilisateur.entity.RoleUtilisateur;
import com.backend.modules.utilisateur.repository.PermissionRepository;
import com.backend.modules.utilisateur.repository.RolePermissionRepository;
import com.backend.modules.utilisateur.repository.UtilisateurPermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Les défauts de rôle et les écarts individuels sont relus à chaque appel.
 */
@Service
@RequiredArgsConstructor
public class AutorisationService {

    private static final List<String> ATTENDUES = List.of(
            "EMPLOYES_CONSULTER", "EMPLOYES_GERER",
            "DEMANDES_CONSULTER", "DEMANDES_SUIVI", "DEMANDES_STATUT",
            "CATALOGUE_CONSULTER", "CATALOGUE_GERER",
            "ECHANTILLONS_GERER", "ESSAIS_GERER",
            "VALIDATION_GERER", "RAPPORTS_GERER",
            "FACTURATION_CONSULTER", "FACTURATION_GERER",
            "PLANIFICATION_GERER", "PLANIFICATION_STATUT",
            "STOCK_CONSULTER", "STOCK_GERER",
            "CONFORMITE_CONSULTER", "CONFORMITE_DECLARER", "CONFORMITE_GERER",
            "IA_GERER", "SATISFACTION_CONSULTER", "NOTIFICATIONS_GERER",
            "FACTURATION_PAYER", "PARAMETRES_LABORATOIRE_GERER", "AUDIT_CONSULTER",
            "INDICATEURS_CONSULTER", "EXPORT_DEMANDES", "EXPORT_ENQUETES",
            "EXPORT_FACTURES", "EXPORT_PRODUITS", "EXPORT_EMPLOYES", "EXPORT_AUDIT");

    private static final Map<String, String> EXIGE = Map.of(
            "EMPLOYES_GERER", "EMPLOYES_CONSULTER",
            "CATALOGUE_GERER", "CATALOGUE_CONSULTER",
            "FACTURATION_GERER", "FACTURATION_CONSULTER",
            "STOCK_GERER", "STOCK_CONSULTER",
            "CONFORMITE_GERER", "CONFORMITE_CONSULTER",
            "CONFORMITE_DECLARER", "CONFORMITE_CONSULTER",
            "DEMANDES_STATUT", "DEMANDES_SUIVI");

    private final RolePermissionRepository rolePermissionRepository;
    private final UtilisateurPermissionRepository utilisateurPermissionRepository;
    private final PermissionRepository permissionRepository;

    private final Map<String, String> libelles = new HashMap<>();

    @EventListener(ApplicationReadyEvent.class)
    @Transactional(readOnly = true)
    public void verifierCatalogue() {
        libelles.clear();
        for (Permission permission : permissionRepository.findAll()) {
            libelles.put(permission.getCode(), permission.getLibelle());
        }
        List<String> manquantes = new ArrayList<>();
        for (String code : ATTENDUES) {
            if (!libelles.containsKey(code)) {
                manquantes.add(code);
            }
        }
        if (!manquantes.isEmpty()) {
            throw new IllegalStateException(
                    "Catalogue de permissions incomplet. Permissions absentes : "
                            + String.join(", ", manquantes)
                            + ". Appliquez la migration V37 avant de démarrer.");
        }
    }

    public boolean personnalisable(RoleUtilisateur role) {
        return role != RoleUtilisateur.SUPER_ADMINISTRATEUR && role != RoleUtilisateur.CLIENT;
    }

    @Transactional(readOnly = true)
    public Set<String> effectives(Long utilisateurId, String role) {
        if (role == null || !personnalisable(RoleUtilisateur.valueOf(role))) {
            return Set.of();
        }
        Set<String> effectives = new HashSet<>(rolePermissionRepository.listerCodesParRole(role));
        for (Object[] ecart : utilisateurPermissionRepository.listerEcarts(utilisateurId)) {
            String code = (String) ecart[1];
            if (ecart[0] == EffetPermission.AJOUT) {
                effectives.add(code);
            } else {
                effectives.remove(code);
            }
        }
        return Set.copyOf(effectives);
    }

    @Transactional(readOnly = true)
    public Set<String> defauts(String role) {
        return Set.copyOf(rolePermissionRepository.listerCodesParRole(role));
    }

    @Transactional(readOnly = true)
    public List<Permission> catalogue() {
        return permissionRepository.findAll();
    }

    public void verifierCoherence(Collection<String> effectives) {
        for (Map.Entry<String, String> exigence : EXIGE.entrySet()) {
            if (effectives.contains(exigence.getKey()) && !effectives.contains(exigence.getValue())) {
                throw new BusinessRuleException("PERMISSIONS_INCOHERENTES",
                        "« " + libelle(exigence.getKey()) + " » exige « " + libelle(exigence.getValue()) + " ».");
            }
        }
    }

    public String libelle(String code) {
        return libelles.getOrDefault(code, code);
    }

    public Map<String, Long> idsParCode() {
        Map<String, Long> ids = new LinkedHashMap<>();
        for (Permission permission : permissionRepository.findAll()) {
            ids.put(permission.getCode(), permission.getId());
        }
        return ids;
    }
}
