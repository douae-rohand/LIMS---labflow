package com.backend.modules.catalogue.service;

import com.backend.common.audit.JournalAuditService;
import com.backend.common.exception.BusinessRuleException;
import com.backend.common.exception.ResourceNotFoundException;
import com.backend.common.security.SecurityUtils;
import com.backend.common.tenant.TenantContext;
import com.backend.modules.catalogue.dto.*;
import com.backend.modules.domaine.entity.Domaine;
import com.backend.modules.domaine.repository.DomaineRepository;
import com.backend.modules.essai.entity.Essai;
import com.backend.modules.essai.entity.ExigenceEchantillon;
import com.backend.modules.essai.repository.EssaiRepository;
import com.backend.modules.essai.repository.ExigenceEchantillonRepository;
import com.backend.modules.essai.repository.LigneEssaiRepository;
import com.backend.modules.panier.repository.PanierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogueAdminService {

    private final EssaiRepository essaiRepository;
    private final DomaineRepository domaineRepository;
    private final ExigenceEchantillonRepository exigenceEchantillonRepository;
    private final LigneEssaiRepository ligneEssaiRepository;
    private final PanierRepository panierRepository;
    private final JournalAuditService journalAuditService;

    @Transactional(readOnly = true)
    public Page<EssaiAdminDto> listerEssais(Long domaineId, Boolean actif, String q, Pageable pageable) {
        Page<Essai> page;
        if (domaineId != null) {
            page = essaiRepository.findByDomaine_IdAndActif(domaineId, actif != null ? actif : true, pageable);
        } else if (q != null && !q.isBlank()) {
            page = essaiRepository.rechercher(q, actif != null, actif != null ? actif : true, pageable);
        } else {
            page = essaiRepository.findByActif(actif != null ? actif : true, pageable);
        }
        return page.map(this::toEssaiAdminDto);
    }

    @Transactional(readOnly = true)
    public EssaiAdminDto getEssai(Long id) {
        Essai essai = essaiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Essai", "id", id));
        return toEssaiAdminDto(essai);
    }

    @Transactional
    public EssaiAdminDto creerEssai(CreerEssaiRequest request) {
        if (essaiRepository.existsByCode(request.getCode())) {
            throw new BusinessRuleException("ESSAI_CODE_EXISTANT",
                    "Un essai avec ce code existe déjà");
        }

        Domaine domaine = domaineRepository.findById(request.getDomaineId())
                .orElseThrow(() -> new ResourceNotFoundException("Domaine", "id", request.getDomaineId()));

        Essai essai = Essai.builder()
                .code(request.getCode().toUpperCase())
                .designation(request.getDesignation())
                .description(request.getDescription())
                .methode(request.getMethode())
                .tarif(request.getTarif())
                .dureeEstimee(request.getDureeEstimee())
                .unite(request.getUnite())
                .limiteMin(request.getLimiteMin())
                .limiteMax(request.getLimiteMax())
                .seuilCritiqueMin(request.getSeuilCritiqueMin())
                .seuilCritiqueMax(request.getSeuilCritiqueMax())
                .actif(true)
                .domaine(domaine)
                .build();

        essai = essaiRepository.save(essai);

        if (request.getExigences() != null && !request.getExigences().isEmpty()) {
            for (CreerExigenceRequest exigenceReq : request.getExigences()) {
                if (exigenceEchantillonRepository.existsByEssai_IdAndNature(essai.getId(), exigenceReq.getNature())) {
                    throw new BusinessRuleException("EXIGENCE_DEJA_EXISTANTE",
                            "Une exigence avec cette nature existe déjà pour cet essai");
                }
                ExigenceEchantillon exigence = ExigenceEchantillon.builder()
                        .essai(essai)
                        .nature(exigenceReq.getNature())
                        .etatConservation(exigenceReq.getEtatConservation())
                        .quantiteMinimale(exigenceReq.getQuantiteMinimale())
                        .unite(exigenceReq.getUnite())
                        .build();
                exigenceEchantillonRepository.save(exigence);
            }
        }

        journalAuditService.enregistrer("CREATION_ESSAI", "Essai", essai.getId(),
                SecurityUtils.principalCourant().getEmail(), null, "{\"tarif\": " + essai.getTarif() + ", \"actif\": true}", null);

        return toEssaiAdminDto(essai);
    }

    @Transactional
    public EssaiAdminDto modifierEssai(Long id, ModifierEssaiRequest request) {
        Essai essai = essaiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Essai", "id", id));

        String valeurAvant = "{\"tarif\": " + essai.getTarif() + ", \"actif\": " + essai.getActif() + "}";

        if (request.getDesignation() != null) essai.setDesignation(request.getDesignation());
        if (request.getDescription() != null) essai.setDescription(request.getDescription());
        if (request.getMethode() != null) essai.setMethode(request.getMethode());
        if (request.getTarif() != null) essai.setTarif(request.getTarif());
        if (request.getDureeEstimee() != null) essai.setDureeEstimee(request.getDureeEstimee());
        if (request.getUnite() != null) essai.setUnite(request.getUnite());
        if (request.getLimiteMin() != null) essai.setLimiteMin(request.getLimiteMin());
        if (request.getLimiteMax() != null) essai.setLimiteMax(request.getLimiteMax());
        if (request.getSeuilCritiqueMin() != null) essai.setSeuilCritiqueMin(request.getSeuilCritiqueMin());
        if (request.getSeuilCritiqueMax() != null) essai.setSeuilCritiqueMax(request.getSeuilCritiqueMax());
        if (request.getDomaineId() != null) {
            Domaine domaine = domaineRepository.findById(request.getDomaineId())
                    .orElseThrow(() -> new ResourceNotFoundException("Domaine", "id", request.getDomaineId()));
            essai.setDomaine(domaine);
        }

        essai = essaiRepository.save(essai);

        String valeurApres = "{\"tarif\": " + essai.getTarif() + ", \"actif\": " + essai.getActif() + "}";

        journalAuditService.enregistrer("MODIFICATION_ESSAI", "Essai", essai.getId(),
                SecurityUtils.principalCourant().getEmail(), valeurAvant, valeurApres, null);

        return toEssaiAdminDto(essai);
    }

    @Transactional
    public EssaiAdminDto activerEssai(Long id) {
        Essai essai = essaiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Essai", "id", id));

        String valeurAvant = "{\"tarif\": " + essai.getTarif() + ", \"actif\": " + essai.getActif() + "}";
        essai.setActif(true);
        essai = essaiRepository.save(essai);
        String valeurApres = "{\"tarif\": " + essai.getTarif() + ", \"actif\": " + essai.getActif() + "}";

        journalAuditService.enregistrer("ACTIVATION_ESSAI", "Essai", essai.getId(),
                SecurityUtils.principalCourant().getEmail(), valeurAvant, valeurApres, null);

        return toEssaiAdminDto(essai);
    }

    @Transactional
    public EssaiAdminDto desactiverEssai(Long id) {
        Essai essai = essaiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Essai", "id", id));

        if (ligneEssaiRepository.existsByEssaiId(id)) {
            throw new BusinessRuleException("ESSAI_UTILISE",
                    "Cet essai est utilisé dans des lignes d'essai et ne peut être que désactivé");
        }

        String valeurAvant = "{\"tarif\": " + essai.getTarif() + ", \"actif\": " + essai.getActif() + "}";
        essai.setActif(false);
        essai = essaiRepository.save(essai);
        String valeurApres = "{\"tarif\": " + essai.getTarif() + ", \"actif\": " + essai.getActif() + "}";

        journalAuditService.enregistrer("DESACTIVATION_ESSAI", "Essai", essai.getId(),
                SecurityUtils.principalCourant().getEmail(), valeurAvant, valeurApres, null);

        return toEssaiAdminDto(essai);
    }

    @Transactional
    public void supprimerEssai(Long id) {
        Essai essai = essaiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Essai", "id", id));

        if (ligneEssaiRepository.existsByEssaiId(id)) {
            throw new BusinessRuleException("ESSAI_UTILISE",
                    "Cet essai est utilisé dans des lignes d'essai et ne peut être supprimé");
        }

        if (panierRepository.existsByEssaiId(id)) {
            throw new BusinessRuleException("ESSAI_UTILISE",
                    "Cet essai est utilisé dans des paniers et ne peut être supprimé");
        }

        essaiRepository.delete(essai);

        journalAuditService.enregistrer("SUPPRESSION_ESSAI", "Essai", id,
                SecurityUtils.principalCourant().getEmail(), null, null, null);
    }

    @Transactional(readOnly = true)
    public List<DomaineDto> listerDomaines() {
        return domaineRepository.findAllByOrderByLibelleAsc().stream()
                .map(d -> DomaineDto.builder()
                        .id(d.getId())
                        .code(d.getCode())
                        .libelle(d.getLibelle())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public DomaineDto creerDomaine(CreerDomaineRequest request) {
        if (domaineRepository.findByCodeIgnoreCase(request.getCode()).isPresent()) {
            throw new BusinessRuleException("DOMAINE_CODE_EXISTANT",
                    "Un domaine avec ce code existe déjà");
        }

        Domaine domaine = Domaine.builder()
                .code(request.getCode())
                .libelle(request.getLibelle())
                .build();

        domaine = domaineRepository.save(domaine);

        journalAuditService.enregistrer("CREATION_DOMAINE", "Domaine", domaine.getId(),
                SecurityUtils.principalCourant().getEmail(), null, null, null);

        return DomaineDto.builder()
                .id(domaine.getId())
                .code(domaine.getCode())
                .libelle(domaine.getLibelle())
                .build();
    }

    @Transactional
    public DomaineDto modifierDomaine(Long id, ModifierDomaineRequest request) {
        Domaine domaine = domaineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Domaine", "id", id));

        if (request.getLibelle() != null) {
            domaine.setLibelle(request.getLibelle());
        }

        domaine = domaineRepository.save(domaine);

        journalAuditService.enregistrer("MODIFICATION_DOMAINE", "Domaine", domaine.getId(),
                SecurityUtils.principalCourant().getEmail(), null, null, null);

        return DomaineDto.builder()
                .id(domaine.getId())
                .code(domaine.getCode())
                .libelle(domaine.getLibelle())
                .build();
    }

    @Transactional
    public void supprimerDomaine(Long id) {
        Domaine domaine = domaineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Domaine", "id", id));

        if (essaiRepository.existsByDomaine_Id(id)) {
            throw new BusinessRuleException("DOMAINE_NON_VIDE",
                    "Ce domaine contient des essais et ne peut être supprimé");
        }

        domaineRepository.delete(domaine);

        journalAuditService.enregistrer("SUPPRESSION_DOMAINE", "Domaine", id,
                SecurityUtils.principalCourant().getEmail(), null, null, null);
    }

    @Transactional(readOnly = true)
    public List<ExigenceEchantillonDto> listerExigences(Long essaiId) {
        return exigenceEchantillonRepository.findByEssai_Id(essaiId).stream()
                .map(e -> ExigenceEchantillonDto.builder()
                        .id(e.getId())
                        .essaiId(e.getEssai().getId())
                        .nature(e.getNature())
                        .etatConservation(e.getEtatConservation())
                        .quantiteMinimale(e.getQuantiteMinimale())
                        .unite(e.getUnite())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public ExigenceEchantillonDto creerExigence(CreerExigenceRequest request, Long essaiId) {
        Essai essai = essaiRepository.findById(essaiId)
                .orElseThrow(() -> new ResourceNotFoundException("Essai", "id", essaiId));

        if (exigenceEchantillonRepository.existsByEssai_IdAndNature(essaiId, request.getNature())) {
            throw new BusinessRuleException("EXIGENCE_DEJA_EXISTANTE",
                    "Une exigence avec cette nature existe déjà pour cet essai");
        }

        ExigenceEchantillon exigence = ExigenceEchantillon.builder()
                .essai(essai)
                .nature(request.getNature())
                .etatConservation(request.getEtatConservation())
                .quantiteMinimale(request.getQuantiteMinimale())
                .unite(request.getUnite())
                .build();

        exigence = exigenceEchantillonRepository.save(exigence);

        return ExigenceEchantillonDto.builder()
                .id(exigence.getId())
                .essaiId(exigence.getEssai().getId())
                .nature(exigence.getNature())
                .etatConservation(exigence.getEtatConservation())
                .quantiteMinimale(exigence.getQuantiteMinimale())
                .unite(exigence.getUnite())
                .build();
    }

    @Transactional
    public ExigenceEchantillonDto modifierExigence(Long id, CreerExigenceRequest request) {
        ExigenceEchantillon exigence = exigenceEchantillonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ExigenceEchantillon", "id", id));

        exigence.setNature(request.getNature());
        exigence.setEtatConservation(request.getEtatConservation());
        exigence.setQuantiteMinimale(request.getQuantiteMinimale());
        exigence.setUnite(request.getUnite());

        exigence = exigenceEchantillonRepository.save(exigence);

        return ExigenceEchantillonDto.builder()
                .id(exigence.getId())
                .essaiId(exigence.getEssai().getId())
                .nature(exigence.getNature())
                .etatConservation(exigence.getEtatConservation())
                .quantiteMinimale(exigence.getQuantiteMinimale())
                .unite(exigence.getUnite())
                .build();
    }

    @Transactional
    public void supprimerExigence(Long id) {
        ExigenceEchantillon exigence = exigenceEchantillonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ExigenceEchantillon", "id", id));

        exigenceEchantillonRepository.delete(exigence);
    }

    @Transactional(readOnly = true)
    public CatalogueStatsDto getStats() {
        long totalEssais = essaiRepository.count();
        long essaisActifs = essaiRepository.countByActifTrue();
        long essaisInactifs = totalEssais - essaisActifs;

        List<Domaine> domaines = domaineRepository.findAll();
        Map<String, Long> essaisParDomaine = new HashMap<>();
        for (Domaine domaine : domaines) {
            long count = essaiRepository.countByDomaine_IdAndActifTrue(domaine.getId());
            essaisParDomaine.put(domaine.getLibelle(), count);
        }

        return CatalogueStatsDto.builder()
                .totalEssais(totalEssais)
                .essaisActifs(essaisActifs)
                .essaisInactifs(essaisInactifs)
                .essaisParDomaine(essaisParDomaine)
                .build();
    }

    private EssaiAdminDto toEssaiAdminDto(Essai essai) {
        List<ExigenceEchantillonDto> exigences = exigenceEchantillonRepository.findByEssai_Id(essai.getId()).stream()
                .map(e -> ExigenceEchantillonDto.builder()
                        .id(e.getId())
                        .essaiId(e.getEssai().getId())
                        .nature(e.getNature())
                        .etatConservation(e.getEtatConservation())
                        .quantiteMinimale(e.getQuantiteMinimale())
                        .unite(e.getUnite())
                        .build())
                .collect(Collectors.toList());

        return EssaiAdminDto.builder()
                .id(essai.getId())
                .code(essai.getCode())
                .designation(essai.getDesignation())
                .description(essai.getDescription())
                .methode(essai.getMethode())
                .tarif(essai.getTarif())
                .dureeEstimee(essai.getDureeEstimee())
                .unite(essai.getUnite())
                .limiteMin(essai.getLimiteMin())
                .limiteMax(essai.getLimiteMax())
                .seuilCritiqueMin(essai.getSeuilCritiqueMin())
                .seuilCritiqueMax(essai.getSeuilCritiqueMax())
                .actif(essai.getActif())
                .domaineId(essai.getDomaine() != null ? essai.getDomaine().getId() : null)
                .domaineLibelle(essai.getDomaine() != null ? essai.getDomaine().getLibelle() : null)
                .exigences(exigences)
                .build();
    }
}
