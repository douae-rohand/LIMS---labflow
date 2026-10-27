package com.backend.modules.catalogue.service;

import com.backend.common.dto.PageResponse;
import com.backend.common.exception.BusinessRuleException;
import com.backend.common.exception.ResourceNotFoundException;
import com.backend.common.exception.UnauthorizedTenantException;
import com.backend.common.tenant.TenantContext;
import com.backend.modules.catalogue.dto.AnalyseTypeDto;
import com.backend.modules.catalogue.dto.DomaineCatalogueDto;
import com.backend.modules.catalogue.dto.EnregistrerAnalyseRequest;
import com.backend.modules.domaine.entity.Domaine;
import com.backend.modules.domaine.repository.DomaineRepository;
import com.backend.modules.essai.entity.Essai;
import com.backend.modules.essai.repository.EssaiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CatalogueService {

    private static final Set<String> STATUTS = Set.of("ACTIF", "INACTIF", "TOUS");
    private static final int TAILLE_MAX = 50;

    private final EssaiRepository essaiRepository;
    private final DomaineRepository domaineRepository;

    @Transactional(readOnly = true)
    public PageResponse<AnalyseTypeDto> lister(String q, String statut, boolean actifSeulement, int page, int size) {
        exigerCatalogueDuLaboratoire();
        Boolean actif = resoudreFiltreActif(statut, actifSeulement);
        int index = Math.max(page, 0);
        int taille = Math.min(Math.max(size, 1), TAILLE_MAX);
        var resultat = essaiRepository.rechercher(
                normaliserRecherche(q),
                actif != null,
                Boolean.TRUE.equals(actif),
                PageRequest.of(index, taille, Sort.by(Sort.Direction.ASC, "designation")));
        return PageResponse.from(resultat.map(this::toDto));
    }

    @Transactional(readOnly = true)
    public AnalyseTypeDto trouverParCode(String code) {
        exigerCatalogueDuLaboratoire();
        return toDto(essaiRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Essai", "code", code)));
    }

    @Transactional(readOnly = true)
    public List<DomaineCatalogueDto> listerDomaines() {
        exigerCatalogueDuLaboratoire();
        return domaineRepository.findAllByOrderByLibelleAsc().stream()
                .map(domaine -> DomaineCatalogueDto.builder()
                        .code(domaine.getCode())
                        .libelle(domaine.getLibelle())
                        .build())
                .toList();
    }

    @Transactional
    public AnalyseTypeDto creer(EnregistrerAnalyseRequest request) {
        exigerCatalogueDuLaboratoire();
        String code = normaliserCode(request.getCode());
        if (essaiRepository.existsByCodeIgnoreCase(code)) {
            throw new BusinessRuleException(
                    "CODE_ANALYSE_EXISTANT",
                    "Ce code est déjà utilisé dans le catalogue de votre laboratoire.");
        }
        Essai essai = Essai.builder()
                .code(code)
                .designation(request.getDesignation().trim())
                .description(videVersNull(request.getDescription()))
                .methode(videVersNull(request.getNormeReference()))
                .tarif(prix(request))
                .dureeEstimee(request.dureeEffective())
                .unite(videVersNull(request.getUniteMesure()))
                .actif(true)
                .domaine(resoudreDomaine(request.getDomaineCode()))
                .build();
        return toDto(essaiRepository.save(essai));
    }

    @Transactional
    public AnalyseTypeDto modifier(Long id, EnregistrerAnalyseRequest request) {
        exigerCatalogueDuLaboratoire();
        Essai essai = exigerEssai(id);
        String code = normaliserCode(request.getCode());
        if (essaiRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new BusinessRuleException(
                    "CODE_ANALYSE_EXISTANT",
                    "Ce code est déjà utilisé dans le catalogue de votre laboratoire.");
        }
        essai.setCode(code);
        essai.setDesignation(request.getDesignation().trim());
        essai.setDescription(videVersNull(request.getDescription()));
        essai.setMethode(videVersNull(request.getNormeReference()));
        essai.setTarif(prix(request));
        essai.setDureeEstimee(request.dureeEffective());
        essai.setUnite(videVersNull(request.getUniteMesure()));
        essai.setDomaine(resoudreDomaine(request.getDomaineCode()));
        return toDto(essaiRepository.save(essai));
    }

    @Transactional
    public AnalyseTypeDto desactiver(Long id) {
        return changerActivation(id, false);
    }

    @Transactional
    public AnalyseTypeDto activer(Long id) {
        return changerActivation(id, true);
    }

    private AnalyseTypeDto changerActivation(Long id, boolean actif) {
        exigerCatalogueDuLaboratoire();
        Essai essai = exigerEssai(id);
        essai.setActif(actif);
        return toDto(essaiRepository.save(essai));
    }

    private Essai exigerEssai(Long id) {
        return essaiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Essai", "id", id));
    }

    /**
     * Le catalogue n'existe que dans le schéma du laboratoire rattaché au compte.
     * Sans tenant (super-admin, client sans laboratoire), aucune ligne n'est lue ni écrite.
     */
    private void exigerCatalogueDuLaboratoire() {
        if (!StringUtils.hasText(TenantContext.getCurrentTenant())) {
            throw new UnauthorizedTenantException(
                    "Le catalogue se gère uniquement dans le laboratoire rattaché à votre compte.");
        }
    }

    private Boolean resoudreFiltreActif(String statut, boolean actifSeulement) {
        if (StringUtils.hasText(statut)) {
            String normalise = statut.trim().toUpperCase(Locale.ROOT);
            if (!STATUTS.contains(normalise)) {
                throw new BusinessRuleException("STATUT_CATALOGUE_INVALIDE", "Statut de catalogue invalide.");
            }
            return switch (normalise) {
                case "ACTIF" -> Boolean.TRUE;
                case "INACTIF" -> Boolean.FALSE;
                default -> null;
            };
        }
        return actifSeulement ? Boolean.TRUE : null;
    }

    private Domaine resoudreDomaine(String code) {
        if (!StringUtils.hasText(code)) {
            return null;
        }
        return domaineRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new BusinessRuleException(
                        "DOMAINE_INCONNU",
                        "Ce domaine n'existe pas dans votre laboratoire."));
    }

    private String normaliserCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private String normaliserRecherche(String q) {
        if (!StringUtils.hasText(q)) {
            return "";
        }
        String texte = q.trim();
        if (texte.length() > 100) {
            throw new BusinessRuleException("RECHERCHE_TROP_LONGUE", "La recherche est limitée à 100 caractères.");
        }
        return texte;
    }

    private BigDecimal prix(EnregistrerAnalyseRequest request) {
        return request.getPrixUnitaire() == null ? BigDecimal.ZERO : request.getPrixUnitaire();
    }

    private String videVersNull(String valeur) {
        if (!StringUtils.hasText(valeur)) {
            return null;
        }
        return valeur.trim();
    }

    private AnalyseTypeDto toDto(Essai essai) {
        Domaine domaine = essai.getDomaine();
        return AnalyseTypeDto.builder()
                .id(essai.getId())
                .code(essai.getCode())
                .designation(essai.getDesignation())
                .description(essai.getDescription())
                .normeReference(essai.getMethode())
                .dureeEstimee(essai.getDureeEstimee())
                .delaiJours(essai.getDureeEstimee())
                .prixUnitaire(essai.getTarif())
                .uniteMesure(essai.getUnite())
                .domaineCode(domaine == null ? null : domaine.getCode())
                .domaineLibelle(domaine == null ? null : domaine.getLibelle())
                .actif(Boolean.TRUE.equals(essai.getActif()))
                .build();
    }
}
