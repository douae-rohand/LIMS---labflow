package com.backend.modules.client.service;

import com.backend.common.exception.BusinessRuleException;
import com.backend.common.exception.ResourceNotFoundException;
import com.backend.common.tenant.TenantExecutor;
import com.backend.modules.client.dto.ClientProfilDto;
import com.backend.modules.client.dto.ModifierClientProfilRequest;
import com.backend.modules.client.entity.Client;
import com.backend.modules.client.entity.ClientLaboratoire;
import com.backend.modules.client.entity.ClientProfil;
import com.backend.modules.client.repository.ClientLaboratoireRepository;
import com.backend.modules.client.repository.ClientProfilRepository;
import com.backend.modules.client.repository.ClientRepository;
import com.backend.modules.plateforme.dto.LaboratoirePublicDto;
import com.backend.modules.plateforme.entity.Laboratoire;
import com.backend.modules.plateforme.repository.LaboratoireRepository;
import com.backend.modules.utilisateur.entity.Utilisateur;
import com.backend.modules.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientProfilRepository clientProfilRepository;
    private final ClientLaboratoireRepository clientLaboratoireRepository;
    private final ClientRepository clientRepository;
    private final LaboratoireRepository laboratoireRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final TenantExecutor tenantExecutor;

    @Transactional
    public ClientProfil creerProfil(Utilisateur utilisateur, String raisonSociale, String ice,
                                    String adresse, Boolean consentementCndp) {
        if (clientProfilRepository.existsByUtilisateur_Id(utilisateur.getId())) {
            return clientProfilRepository.findByUtilisateur_Id(utilisateur.getId()).orElseThrow();
        }
        return clientProfilRepository.save(ClientProfil.builder()
                .utilisateur(utilisateur)
                .raisonSociale(raisonSociale)
                .ice(ice)
                .adresse(adresse)
                .consentementCndp(Boolean.TRUE.equals(consentementCndp))
                .dateConsentementCndp(Boolean.TRUE.equals(consentementCndp) ? Instant.now() : null)
                .dateCreation(Instant.now())
                .build());
    }

    @Transactional(readOnly = true)
    public ClientProfilDto lireProfil(Long utilisateurId) {
        return toDto(chargerProfil(utilisateurId));
    }

    @Transactional
    public ClientProfilDto modifierProfil(Long utilisateurId, ModifierClientProfilRequest request) {
        ClientProfil profil = chargerProfil(utilisateurId);
        if (request.getTypeClient() != null) {
            profil.setTypeClient(request.getTypeClient());
        }
        if (request.getRaisonSociale() != null) {
            profil.setRaisonSociale(request.getRaisonSociale());
        }
        if (request.getIce() != null) {
            profil.setIce(request.getIce());
        }
        if (request.getAdresse() != null) {
            profil.setAdresse(request.getAdresse());
        }
        if (request.getConsentementCndp() != null) {
            profil.setConsentementCndp(request.getConsentementCndp());
            if (request.getConsentementCndp() && profil.getDateConsentementCndp() == null) {
                profil.setDateConsentementCndp(Instant.now());
            }
        }
        return toDto(clientProfilRepository.save(profil));
    }

    @Transactional(readOnly = true)
    public List<LaboratoirePublicDto> listerLaboratoiresDuClient(Long utilisateurId) {
        return clientLaboratoireRepository.findByUtilisateur_Id(utilisateurId).stream()
                .map(ClientLaboratoire::getLaboratoire)
                .map(this::toPublicDto)
                .toList();
    }

    /**
     * Find-or-create de la fiche tenant et du rattachement central. Idempotent.
     */
    public Client assurerFicheLocale(Long utilisateurId, String nomSchema) {
        ClientProfil profil = tenantExecutor.inCentral(() -> chargerProfil(utilisateurId));
        Laboratoire laboratoire = tenantExecutor.inCentral(() -> laboratoireRepository.findByNomSchema(nomSchema)
                .orElseThrow(() -> new ResourceNotFoundException("Laboratoire", "nomSchema", nomSchema)));
        if (!"ACTIF".equals(laboratoire.getStatut())) {
            throw new BusinessRuleException("LABORATOIRE_NON_SELECTIONNABLE",
                    "Le laboratoire n'accepte pas de nouvelles demandes");
        }

        Client fiche = tenantExecutor.inTenant(nomSchema, () -> trouverOuCreerFiche(utilisateurId, profil));

        tenantExecutor.inCentral(() -> {
            if (clientLaboratoireRepository
                    .findByUtilisateur_IdAndLaboratoire_Id(utilisateurId, laboratoire.getId())
                    .isEmpty()) {
                Utilisateur utilisateur = utilisateurRepository.getReferenceById(utilisateurId);
                Laboratoire labRef = laboratoireRepository.getReferenceById(laboratoire.getId());
                clientLaboratoireRepository.save(ClientLaboratoire.builder()
                        .utilisateur(utilisateur)
                        .laboratoire(labRef)
                        .clientLocalId(fiche.getId())
                        .statut("ACTIF")
                        .datePremierContact(Instant.now())
                        .build());
            }
            return null;
        });
        return fiche;
    }

    private Client trouverOuCreerFiche(Long utilisateurId, ClientProfil profil) {
        return clientRepository.findByUtilisateurId(utilisateurId).orElseGet(() -> {
            // Fallback : pour un particulier, raisonSociale est null → utiliser "prénom nom"
            String raisonSociale = (profil.getRaisonSociale() != null
                    && !profil.getRaisonSociale().isBlank())
                    ? profil.getRaisonSociale()
                    : profil.getUtilisateur().getNomComplet();
            try {
                return clientRepository.save(Client.builder()
                        .code("CLI-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                        .raisonSociale(raisonSociale)
                        .ice(profil.getIce())
                        .adresse(profil.getAdresse())
                        .consentementCndp(profil.getConsentementCndp())
                        .utilisateurId(utilisateurId)
                        .build());
            } catch (DataIntegrityViolationException ex) {
                return clientRepository.findByUtilisateurId(utilisateurId)
                        .orElseThrow(() -> ex);
            }
        });
    }

    private ClientProfil chargerProfil(Long utilisateurId) {
        return clientProfilRepository.findByUtilisateur_Id(utilisateurId)
                .orElseThrow(() -> new ResourceNotFoundException("ClientProfil", "utilisateurId", utilisateurId));
    }

    private ClientProfilDto toDto(ClientProfil profil) {
        return ClientProfilDto.builder()
                .id(profil.getId())
                .utilisateurId(profil.getUtilisateur() == null ? null : profil.getUtilisateur().getId())
                .typeClient(profil.getTypeClient())
                .raisonSociale(profil.getRaisonSociale())
                .ice(profil.getIce())
                .adresse(profil.getAdresse())
                .consentementCndp(profil.getConsentementCndp())
                .dateConsentementCndp(profil.getDateConsentementCndp())
                .versionConsentementCndp(profil.getVersionConsentementCndp())
                .dateCreation(profil.getDateCreation())
                .build();
    }

    private LaboratoirePublicDto toPublicDto(Laboratoire laboratoire) {
        return LaboratoirePublicDto.builder()
                .id(laboratoire.getId())
                .code(laboratoire.getCode())
                .raisonSociale(laboratoire.getRaisonSociale())
                .ville(laboratoire.getVille())
                .adresse(laboratoire.getAdresse())
                .statut(laboratoire.getStatut())
                .build();
    }
}
