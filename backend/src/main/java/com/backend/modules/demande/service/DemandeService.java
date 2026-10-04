package com.backend.modules.demande.service;

import com.backend.common.exception.BusinessRuleException;
import com.backend.common.exception.ResourceNotFoundException;
import com.backend.modules.client.entity.Client;
import com.backend.modules.client.repository.ClientRepository;
import com.backend.modules.demande.dto.DemandeDto;
import com.backend.modules.demande.entity.Demande;
import com.backend.modules.demande.entity.StatutDemande;
import com.backend.modules.demande.repository.DemandeRepository;
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
public class DemandeService {

    private final DemandeRepository demandeRepository;
    private final ClientRepository clientRepository;

    @Transactional(readOnly = true)
    public DemandeDto trouverParId(Long id) {
        return toDto(charger(id));
    }

    @Transactional(readOnly = true)
    public Page<DemandeDto> listerParClient(Long clientId, Pageable pageable) {
        return demandeRepository.findByClient_Id(clientId, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<DemandeDto> listerParStatut(StatutDemande statut, Pageable pageable) {
        return demandeRepository.findByStatut(statut.name(), pageable).map(this::toDto);
    }

    @Transactional
    public DemandeDto creer(DemandeDto dto) {
        if (dto.getClientId() == null) {
            throw new BusinessRuleException("DEMANDE_SANS_TIERS",
                    "Une demande doit référencer un client ou un patient");
        }
        Client client = clientRepository.findById(dto.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", dto.getClientId()));

        Demande demande = Demande.builder()
                .numero("DEM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .titre(dto.getObjet())
                .objectif(dto.getCommentaire())
                .dateSoumission(Instant.now())
                .statut(StatutDemande.BROUILLON.name())
                .client(client)
                .build();
        return toDto(demandeRepository.save(demande));
    }

    @Transactional
    public DemandeDto soumettre(Long id) {
        Demande demande = charger(id);
        demande.setStatut(StatutDemande.SOUMISE.name());
        demande.setDateSoumission(Instant.now());
        return toDto(demandeRepository.save(demande));
    }

    @Transactional
    public DemandeDto changerStatut(Long id, StatutDemande statut) {
        Demande demande = charger(id);
        demande.setStatut(statut.name());
        return toDto(demandeRepository.save(demande));
    }

    private Demande charger(Long id) {
        return demandeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Demande", "id", id));
    }

    private DemandeDto toDto(Demande d) {
        Long clientId = d.getClient() == null ? null : d.getClient().getId();
        return DemandeDto.builder()
                .id(d.getId())
                .reference(d.getNumero())
                .objet(d.getTitre())
                .statut(enumOuNull(StatutDemande.class, d.getStatut()))
                .clientId(clientId)
                .dateSoumission(d.getDateSoumission())
                .commentaire(d.getObjectif())
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
