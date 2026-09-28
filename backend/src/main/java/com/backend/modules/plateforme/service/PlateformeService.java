package com.backend.modules.plateforme.service;

import com.backend.common.exception.BusinessRuleException;
import com.backend.common.exception.ResourceNotFoundException;
import com.backend.modules.plateforme.dto.*;
import com.backend.modules.plateforme.entity.*;
import com.backend.modules.plateforme.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service de gestion de la plateforme (M15) — schéma central.
 *
 * <p>Responsabilités :
 * <ul>
 *   <li>Création et gestion des laboratoires (tenants)</li>
 *   <li>Traitement des demandes d'intégration</li>
 * </ul>
 *
 * <p>TODO : à la création d'un laboratoire, provisionner dynamiquement le
 * schéma MySQL {@code lims_<code>} et exécuter les migrations Flyway tenant.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlateformeService {

    private final LaboratoireRepository laboratoireRepository;
    private final DemandeIntegrationRepository demandeIntegrationRepository;

    // -------------------------------------------------------------------------
    // Laboratoires
    // -------------------------------------------------------------------------

    @Transactional
    public LaboratoireDto creerLaboratoire(CreerLaboratoireRequest request) {
        if (laboratoireRepository.existsByCode(request.getCode())) {
            throw new BusinessRuleException("CODE_LABORATOIRE_EXISTANT",
                    "Le code laboratoire '" + request.getCode() + "' est déjà utilisé");
        }

        String schemaName = "lims_" + request.getCode();

        Laboratoire labo = Laboratoire.builder()
                .code(request.getCode())
                .nom(request.getNom())
                .description(request.getDescription())
                .schemaName(schemaName)
                .adresse(request.getAdresse())
                .telephone(request.getTelephone())
                .emailContact(request.getEmailContact())
                .numeroAccreditation(request.getNumeroAccreditation())
                .build();

        labo = laboratoireRepository.save(labo);
        log.info("Laboratoire créé : code={}, schema={}", labo.getCode(), labo.getSchemaName());

        // TODO: provisionner le schéma MySQL et exécuter les migrations Flyway tenant
        // schemaProvisioningService.provisionner(labo.getSchemaName());

        return toDto(labo);
    }

    @Transactional(readOnly = true)
    public LaboratoireDto trouverParCode(String code) {
        return toDto(laboratoireRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Laboratoire", "code", code)));
    }

    @Transactional(readOnly = true)
    public Page<LaboratoireDto> listerLaboratoires(Pageable pageable) {
        return laboratoireRepository.findAll(pageable).map(this::toDto);
    }

    @Transactional
    public void desactiverLaboratoire(Long id) {
        Laboratoire labo = laboratoireRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Laboratoire", "id", id));
        labo.setActif(false);
        laboratoireRepository.save(labo);
        log.info("Laboratoire désactivé : id={}, code={}", id, labo.getCode());
    }

    // -------------------------------------------------------------------------
    // Demandes d'intégration
    // -------------------------------------------------------------------------

    @Transactional
    public DemandeIntegrationDto soumettreDemande(DemandeIntegrationDto dto) {
        DemandeIntegration demande = DemandeIntegration.builder()
                .nomLaboratoire(dto.getNomLaboratoire())
                .emailRepresentant(dto.getEmailRepresentant())
                .nomRepresentant(dto.getNomRepresentant())
                .telephoneRepresentant(dto.getTelephoneRepresentant())
                .message(dto.getMessage())
                .build();

        demande = demandeIntegrationRepository.save(demande);
        log.info("Demande d'intégration soumise : id={}, labo={}", demande.getId(), demande.getNomLaboratoire());
        return toDto(demande);
    }

    @Transactional
    public DemandeIntegrationDto traiterDemande(Long id, StatutIntegration decision,
                                                 String commentaire) {
        DemandeIntegration demande = demandeIntegrationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DemandeIntegration", "id", id));

        demande.setStatut(decision);
        demande.setCommentaireAdmin(commentaire);
        demande.setDateTraitement(java.time.Instant.now());

        // Si approuvée, créer automatiquement le laboratoire
        if (decision == StatutIntegration.APPROUVEE) {
            // TODO: créer le laboratoire depuis les infos de la demande
            log.info("Demande {} approuvée — provisionnement du laboratoire à implémenter", id);
        }

        return toDto(demandeIntegrationRepository.save(demande));
    }

    @Transactional(readOnly = true)
    public Page<DemandeIntegrationDto> listerDemandes(StatutIntegration statut, Pageable pageable) {
        Page<DemandeIntegration> page = (statut != null)
                ? demandeIntegrationRepository.findByStatut(statut, pageable)
                : demandeIntegrationRepository.findAll(pageable);
        return page.map(this::toDto);
    }

    // -------------------------------------------------------------------------
    // Mappers internes
    // -------------------------------------------------------------------------

    private LaboratoireDto toDto(Laboratoire l) {
        return LaboratoireDto.builder()
                .id(l.getId()).code(l.getCode()).nom(l.getNom())
                .description(l.getDescription()).schemaName(l.getSchemaName())
                .adresse(l.getAdresse()).telephone(l.getTelephone())
                .emailContact(l.getEmailContact())
                .numeroAccreditation(l.getNumeroAccreditation())
                .actif(l.isActif()).dateCreation(l.getDateCreation())
                .build();
    }

    private DemandeIntegrationDto toDto(DemandeIntegration d) {
        return DemandeIntegrationDto.builder()
                .id(d.getId()).nomLaboratoire(d.getNomLaboratoire())
                .emailRepresentant(d.getEmailRepresentant())
                .nomRepresentant(d.getNomRepresentant())
                .telephoneRepresentant(d.getTelephoneRepresentant())
                .message(d.getMessage()).statut(d.getStatut())
                .commentaireAdmin(d.getCommentaireAdmin())
                .dateSoumission(d.getDateSoumission())
                .dateTraitement(d.getDateTraitement())
                .build();
    }
}
