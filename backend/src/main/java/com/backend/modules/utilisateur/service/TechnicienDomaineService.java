package com.backend.modules.utilisateur.service;

import com.backend.common.audit.JournalAuditService;
import com.backend.common.exception.BusinessRuleException;
import com.backend.common.security.SecurityUtils;
import com.backend.common.tenant.TenantExecutor;
import com.backend.modules.auth.security.UtilisateurPrincipal;
import com.backend.modules.catalogue.dto.DomaineDto;
import com.backend.modules.domaine.entity.Domaine;
import com.backend.modules.domaine.repository.DomaineRepository;
import com.backend.modules.utilisateur.dto.EmployeDetailDto;
import com.backend.modules.utilisateur.entity.TechnicienDomaine;
import com.backend.modules.utilisateur.repository.TechnicienDomaineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TechnicienDomaineService {

    private static final String ROLE_TECHNICIEN = "TECHNICIEN";

    private final TechnicienDomaineRepository technicienDomaineRepository;
    private final DomaineRepository domaineRepository;
    private final TenantExecutor tenantExecutor;
    private final JournalAuditService journalAuditService;

    public List<DomaineDto> remplacerDomaines(Long id, List<Long> domaineIds, UtilisateurPrincipal principal) {
        List<Long> ids = normaliserIds(domaineIds);
        String schema = schemaCourant(principal);

        ResultatDomaines resultat = tenantExecutor.inTenant(schema, () -> remplacerDansTenant(id, ids));
        if (resultat.modifie()) {
            auditer(id, resultat.idsAvant(), ids, principal);
        }
        return resultat.domaines();
    }

    public List<DomaineDto> domainesTechnicienConnecte() {
        UtilisateurPrincipal principal = SecurityUtils.principalCourant();
        if (!ROLE_TECHNICIEN.equals(principal.getRole().name())) {
            throw new BusinessRuleException("ROLE_TECHNICIEN_REQUIS",
                    "Cette ressource est réservée au technicien connecté.");
        }
        String schema = schemaCourant(principal);
        return tenantExecutor.inTenant(schema,
                () -> dtoAffectations(technicienDomaineRepository.findByTechnicienId(principal.getId())));
    }

    public Map<Long, List<String>> domainesDes(Collection<Long> technicienIds) {
        if (technicienIds.isEmpty()) {
            return Map.of();
        }
        String schema = schemaCourant(SecurityUtils.principalCourant());
        return tenantExecutor.inTenant(schema, () -> {
            List<TechnicienDomaine> affectations =
                    technicienDomaineRepository.findByTechnicienIdIn(technicienIds);
            List<Long> domaineIds = affectations.stream().map(TechnicienDomaine::getDomaineId).distinct().toList();
            Map<Long, String> codes = new HashMap<>();
            domaineRepository.findAllById(domaineIds)
                    .forEach(domaine -> codes.put(domaine.getId(), domaine.getCode()));
            Map<Long, List<String>> result = new HashMap<>();
            for (TechnicienDomaine affectation : affectations) {
                String code = codes.get(affectation.getDomaineId());
                if (code != null) {
                    result.computeIfAbsent(affectation.getTechnicienId(), key -> new ArrayList<>()).add(code);
                }
            }
            result.values().forEach(liste -> liste.sort(String.CASE_INSENSITIVE_ORDER));
            return result;
        });
    }

    public EmployeDetailDto completerDomaines(EmployeDetailDto employe) {
        if (!ROLE_TECHNICIEN.equals(employe.getRole())) {
            employe.setDomaines(List.of());
            return employe;
        }
        String schema = schemaCourant(SecurityUtils.principalCourant());
        employe.setDomaines(tenantExecutor.inTenant(schema,
                () -> dtoAffectations(technicienDomaineRepository.findByTechnicienId(employe.getId()))));
        return employe;
    }

    private ResultatDomaines remplacerDansTenant(Long technicienId, List<Long> ids) {
        List<Domaine> domaines = domaineRepository.findAllById(ids);
        if (domaines.size() != ids.size()) {
            throw new BusinessRuleException("DOMAINE_INTROUVABLE",
                    "Un ou plusieurs domaines n'existent pas dans ce laboratoire.");
        }
        List<Long> idsAvant = idsTries(technicienDomaineRepository.findByTechnicienId(technicienId));
        if (idsAvant.equals(ids)) {
            return new ResultatDomaines(false, idsAvant, dtoDomaines(domaines));
        }
        technicienDomaineRepository.deleteByTechnicienId(technicienId);
        if (!domaines.isEmpty()) {
            technicienDomaineRepository.saveAll(domaines.stream()
                    .map(domaine -> TechnicienDomaine.builder()
                            .technicienId(technicienId)
                            .domaineId(domaine.getId())
                            .build())
                    .toList());
        }
        return new ResultatDomaines(true, idsAvant, dtoDomaines(domaines));
    }

    public void supprimerDomaines(Long technicienId, UtilisateurPrincipal principal) {
        String schema = schemaCourant(principal);
        List<Long> idsAvant = tenantExecutor.inTenant(schema, () -> {
            List<TechnicienDomaine> affectations = technicienDomaineRepository.findByTechnicienId(technicienId);
            List<Long> ids = idsTries(affectations);
            if (!ids.isEmpty()) {
                technicienDomaineRepository.deleteByTechnicienId(technicienId);
            }
            return ids;
        });
        if (!idsAvant.isEmpty()) {
            auditer(technicienId, idsAvant, List.of(), principal);
        }
    }

    private void auditer(Long technicienId, List<Long> avant, List<Long> apres,
                         UtilisateurPrincipal principal) {
        journalAuditService.enregistrer("MODIFICATION_DOMAINES_EMPLOYE", "Utilisateur", technicienId,
                principal.getEmail(), idsJson(avant), idsJson(apres), null, principal.getLaboratoireId());
    }

    private static List<Long> normaliserIds(List<Long> ids) {
        if (ids == null || ids.size() > 50) {
            throw new BusinessRuleException("DOMAINES_INVALIDES",
                    "La liste des domaines doit contenir au maximum 50 identifiants.");
        }
        List<Long> normalises = new ArrayList<>(ids);
        if (normalises.stream().anyMatch(Objects::isNull)) {
            throw new BusinessRuleException("DOMAINES_INVALIDES", "Un identifiant de domaine est invalide.");
        }
        if (normalises.stream().distinct().count() != normalises.size()) {
            throw new BusinessRuleException("DOMAINES_DUPLIQUES", "Un domaine ne peut être sélectionné qu'une fois.");
        }
        normalises.sort(Long::compareTo);
        return normalises;
    }

    private static List<Long> idsTries(List<TechnicienDomaine> affectations) {
        return affectations.stream().map(TechnicienDomaine::getDomaineId).sorted().toList();
    }

    private List<DomaineDto> dtoAffectations(List<TechnicienDomaine> affectations) {
        return dtoDomaines(domaineRepository.findAllById(idsTries(affectations)));
    }

    private List<DomaineDto> dtoDomaines(List<Domaine> domaines) {
        return domaines.stream()
                .sorted(Comparator.comparing(Domaine::getLibelle, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Domaine::getId))
                .map(this::versDomaineDto)
                .toList();
    }

    private DomaineDto versDomaineDto(Domaine domaine) {
        return DomaineDto.builder().id(domaine.getId()).code(domaine.getCode())
                .libelle(domaine.getLibelle()).build();
    }

    private static String idsJson(List<Long> ids) {
        return ids.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(",", "[", "]"));
    }

    private static String schemaCourant(UtilisateurPrincipal principal) {
        if (principal.getNomSchema() == null || principal.getNomSchema().isBlank()) {
            throw new BusinessRuleException("ADMIN_SANS_LABORATOIRE",
                    "Votre compte administrateur n'est pas rattaché à un laboratoire.");
        }
        return principal.getNomSchema();
    }

    private record ResultatDomaines(boolean modifie, List<Long> idsAvant, List<DomaineDto> domaines) {
    }
}
