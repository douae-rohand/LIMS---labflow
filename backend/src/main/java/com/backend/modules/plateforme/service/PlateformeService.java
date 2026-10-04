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

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlateformeService {

    private static final String STATUT_ACTIF = "ACTIF";
    private static final String STATUT_INACTIF = "INACTIF";

    private final LaboratoireRepository laboratoireRepository;
    private final DemandeIntegrationRepository demandeIntegrationRepository;

    @Transactional
    public LaboratoireDto creerLaboratoire(CreerLaboratoireRequest request) {
        if (laboratoireRepository.existsByCode(request.getCode())) {
            throw new BusinessRuleException("CODE_LABORATOIRE_EXISTANT",
                    "Le code laboratoire '" + request.getCode() + "' est déjà utilisé");
        }

        String nomSchema = "lims_" + request.getCode();

        Laboratoire labo = Laboratoire.builder()
                .code(request.getCode())
                .raisonSociale(request.getNom())
                .nomSchema(nomSchema)
                .adresse(request.getAdresse())
                .telephone(request.getTelephone())
                .email(request.getEmailContact())
                .statut(STATUT_ACTIF)
                .build();

        labo = laboratoireRepository.save(labo);
        log.info("Laboratoire créé : code={}, schema={}", labo.getCode(), labo.getNomSchema());

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
        labo.setStatut(STATUT_INACTIF);
        laboratoireRepository.save(labo);
        log.info("Laboratoire désactivé : id={}, code={}", id, labo.getCode());
    }

    @Transactional
    public DemandeIntegrationDto soumettreDemande(DemandeIntegrationDto dto) {
        DemandeIntegration demande = DemandeIntegration.builder()
                .numero("INT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .dateDemande(Instant.now())
                .statut(StatutIntegration.EN_ATTENTE.name())
                .raisonSociale(dto.getNomLaboratoire())
                .contactNom(dto.getNomRepresentant() != null ? dto.getNomRepresentant() : dto.getNomLaboratoire())
                .contactEmail(dto.getEmailRepresentant())
                .contactTelephone(dto.getTelephoneRepresentant())
                .build();

        demande = demandeIntegrationRepository.save(demande);
        log.info("Demande d'intégration soumise : id={}, labo={}", demande.getId(), demande.getRaisonSociale());
        return toDto(demande);
    }

    @Transactional
    public DemandeIntegrationDto traiterDemande(Long id, StatutIntegration decision, String commentaire) {
        DemandeIntegration demande = demandeIntegrationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DemandeIntegration", "id", id));

        demande.setStatut(decision.name());
        if (decision == StatutIntegration.REJETEE) {
            demande.setMotifRefus(commentaire);
        }

        if (decision == StatutIntegration.APPROUVEE) {
            log.info("Demande {} approuvée — provisionnement du laboratoire à implémenter", id);
        }

        return toDto(demandeIntegrationRepository.save(demande));
    }

    @Transactional(readOnly = true)
    public Page<DemandeIntegrationDto> listerDemandes(StatutIntegration statut, Pageable pageable) {
        Page<DemandeIntegration> page = (statut != null)
                ? demandeIntegrationRepository.findByStatut(statut.name(), pageable)
                : demandeIntegrationRepository.findAll(pageable);
        return page.map(this::toDto);
    }

    private LaboratoireDto toDto(Laboratoire l) {
        return LaboratoireDto.builder()
                .id(l.getId())
                .code(l.getCode())
                .nom(l.getRaisonSociale())
                .schemaName(l.getNomSchema())
                .adresse(l.getAdresse())
                .telephone(l.getTelephone())
                .emailContact(l.getEmail())
                .actif(STATUT_ACTIF.equals(l.getStatut()))
                .dateCreation(l.getDateCreation())
                .build();
    }

    private DemandeIntegrationDto toDto(DemandeIntegration d) {
        return DemandeIntegrationDto.builder()
                .id(d.getId())
                .nomLaboratoire(d.getRaisonSociale())
                .emailRepresentant(d.getContactEmail())
                .nomRepresentant(d.getContactNom())
                .telephoneRepresentant(d.getContactTelephone())
                .message(d.getMotifRefus())
                .statut(enumOuNull(StatutIntegration.class, d.getStatut()))
                .commentaireAdmin(d.getMotifRefus())
                .dateSoumission(d.getDateDemande())
                .build();
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
