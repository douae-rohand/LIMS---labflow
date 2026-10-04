package com.backend.modules.demande.service;

import com.backend.common.exception.BusinessRuleException;
import com.backend.common.exception.ResourceNotFoundException;
import com.backend.common.exception.UnauthorizedTenantException;
import com.backend.common.security.SecurityUtils;
import com.backend.common.tenant.TenantContext;
import com.backend.common.tenant.TenantExecutor;
import com.backend.modules.auth.security.UtilisateurPrincipal;
import com.backend.modules.client.entity.Client;
import com.backend.modules.client.entity.ClientLaboratoire;
import com.backend.modules.client.repository.ClientLaboratoireRepository;
import com.backend.modules.client.repository.ClientRepository;
import com.backend.modules.client.service.ClientService;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DemandeService {

    private final DemandeRepository demandeRepository;
    private final ClientRepository clientRepository;
    private final ClientService clientService;
    private final ClientLaboratoireRepository clientLaboratoireRepository;
    private final TenantExecutor tenantExecutor;

    @Transactional(readOnly = true)
    public DemandeDto trouverParId(Long id) {
        Demande demande = charger(id);
        verifierProprieteClient(demande);
        return toDto(demande, TenantContext.getCurrentTenant());
    }

    @Transactional(readOnly = true)
    public Page<DemandeDto> listerParClient(Long clientId, Pageable pageable) {
        return demandeRepository.findByClient_Id(clientId, pageable)
                .map(d -> toDto(d, TenantContext.getCurrentTenant()));
    }

    @Transactional(readOnly = true)
    public Page<DemandeDto> listerMesDemandes(Pageable pageable) {
        UtilisateurPrincipal principal = SecurityUtils.principalCourant();
        return demandeRepository.findByClient_UtilisateurId(principal.getId(), pageable)
                .map(d -> toDto(d, TenantContext.getCurrentTenant()));
    }

    public List<DemandeDto> listerToutesMesDemandes() {
        UtilisateurPrincipal principal = SecurityUtils.principalCourant();
        List<ClientLaboratoire> liaisons = tenantExecutor.inCentral(
                () -> clientLaboratoireRepository.findByUtilisateur_Id(principal.getId()));
        List<DemandeDto> resultats = new ArrayList<>();
        for (ClientLaboratoire liaison : liaisons) {
            String nomSchema = liaison.getLaboratoire().getNomSchema();
            String code = liaison.getLaboratoire().getCode();
            List<DemandeDto> duTenant = tenantExecutor.inTenant(nomSchema, () ->
                    demandeRepository.findByClient_UtilisateurId(principal.getId()).stream()
                            .map(d -> toDto(d, code))
                            .toList());
            resultats.addAll(duTenant);
        }
        resultats.sort(Comparator.comparing(DemandeDto::getDateSoumission,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return resultats;
    }

    @Transactional(readOnly = true)
    public Page<DemandeDto> listerParStatut(StatutDemande statut, Pageable pageable) {
        return demandeRepository.findByStatut(statut.name(), pageable)
                .map(d -> toDto(d, TenantContext.getCurrentTenant()));
    }

    public DemandeDto creer(DemandeDto dto) {
        UtilisateurPrincipal principal = SecurityUtils.principalCourant();
        String nomSchema = TenantContext.getCurrentTenant();
        if (nomSchema == null || nomSchema.isBlank() || "central".equals(nomSchema)) {
            throw new UnauthorizedTenantException("Un laboratoire doit être sélectionné pour créer une demande");
        }
        if (dto.getClientId() != null) {
            throw new BusinessRuleException("CLIENT_ID_INTERDIT",
                    "Le clientId ne peut pas être fourni : il est déduit du compte connecté");
        }
        Client fiche = clientService.assurerFicheLocale(principal.getId(), nomSchema);
        return tenantExecutor.inTenant(nomSchema, () -> {
            Client client = clientRepository.findById(fiche.getId()).orElse(fiche);
            Demande demande = Demande.builder()
                    .numero("DEM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .titre(dto.getObjet())
                    .objectif(dto.getCommentaire())
                    .dateSoumission(Instant.now())
                    .statut(StatutDemande.BROUILLON.name())
                    .client(client)
                    .build();
            return toDto(demandeRepository.save(demande), nomSchema);
        });
    }

    @Transactional
    public DemandeDto soumettre(Long id) {
        Demande demande = charger(id);
        verifierProprieteClient(demande);
        demande.setStatut(StatutDemande.SOUMISE.name());
        demande.setDateSoumission(Instant.now());
        return toDto(demandeRepository.save(demande), TenantContext.getCurrentTenant());
    }

    @Transactional
    public DemandeDto changerStatut(Long id, StatutDemande statut) {
        Demande demande = charger(id);
        demande.setStatut(statut.name());
        return toDto(demandeRepository.save(demande), TenantContext.getCurrentTenant());
    }

    private Demande charger(Long id) {
        return demandeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Demande", "id", id));
    }

    private void verifierProprieteClient(Demande demande) {
        UtilisateurPrincipal principal = SecurityUtils.principalCourant();
        if (!principal.isClient()) {
            return;
        }
        Long proprietaire = demande.getClient() == null ? null : demande.getClient().getUtilisateurId();
        if (proprietaire == null || !proprietaire.equals(principal.getId())) {
            throw new UnauthorizedTenantException("Cette demande ne vous appartient pas");
        }
    }

    private DemandeDto toDto(Demande d, String laboratoireCode) {
        Long clientId = d.getClient() == null ? null : d.getClient().getId();
        return DemandeDto.builder()
                .id(d.getId())
                .reference(d.getNumero())
                .objet(d.getTitre())
                .statut(enumOuNull(StatutDemande.class, d.getStatut()))
                .clientId(clientId)
                .laboratoireCode(laboratoireCode)
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
