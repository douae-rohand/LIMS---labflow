package com.backend.modules.rapport.service;

import com.backend.common.dto.ListeAgregeeDto;
import com.backend.common.exception.ResourceNotFoundException;
import com.backend.common.tenant.TenantExecutor;
import com.backend.modules.client.entity.ClientLaboratoire;
import com.backend.modules.client.repository.ClientLaboratoireRepository;
import com.backend.modules.demande.entity.Demande;
import com.backend.modules.ia.entity.SyntheseIa;
import com.backend.modules.ia.repository.SyntheseIaRepository;
import com.backend.modules.plateforme.entity.Laboratoire;
import com.backend.modules.plateforme.repository.LaboratoireRepository;
import com.backend.modules.rapport.dto.RapportClientDetailDto;
import com.backend.modules.rapport.dto.RapportClientResumeDto;
import com.backend.modules.rapport.dto.RapportLigneClientDto;
import com.backend.modules.rapport.entity.Rapport;
import com.backend.modules.rapport.entity.RapportLigne;
import com.backend.modules.rapport.repository.RapportLigneRepository;
import com.backend.modules.rapport.repository.RapportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Rapports de l'espace client : liste agrégée sur tous les laboratoires du client.
 *
 * <p>Aucune donnée interne (signataire, limites de référence, seuils OOS/OOT,
 * identifiants d'utilisateurs) n'est exposée.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RapportClientService {

    private static final String MENTION_SYNTHESE_IA = "Synthèse automatique, non contractuelle";

    private final RapportRepository rapportRepository;
    private final RapportLigneRepository rapportLigneRepository;
    private final SyntheseIaRepository syntheseIaRepository;
    private final LaboratoireRepository laboratoireRepository;
    private final ClientLaboratoireRepository clientLaboratoireRepository;
    private final TenantExecutor tenantExecutor;

    public ListeAgregeeDto<RapportClientResumeDto> listerRapportsClient(Long utilisateurId) {
        List<ClientLaboratoire> liaisons = tenantExecutor.inCentral(
                () -> clientLaboratoireRepository.findByUtilisateur_Id(utilisateurId));
        List<RapportClientResumeDto> resultats = new ArrayList<>();
        int laboratoiresIndisponibles = 0;
        for (ClientLaboratoire liaison : liaisons) {
            Laboratoire laboratoire = liaison.getLaboratoire();
            try {
                List<RapportClientResumeDto> duLaboratoire = tenantExecutor.inTenant(laboratoire.getNomSchema(),
                        () -> rapportRepository.findVisiblesClient(utilisateurId).stream()
                                .map(rapport -> toResume(rapport, laboratoire))
                                .toList());
                resultats.addAll(duLaboratoire);
            } catch (Exception ex) {
                laboratoiresIndisponibles++;
                log.warn("Rapports indisponibles pour le laboratoire {} : {}",
                        laboratoire.getCode(), ex.getMessage());
            }
        }
        resultats.sort(Comparator.comparing(RapportClientResumeDto::getDateEmission,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return ListeAgregeeDto.de(resultats, laboratoiresIndisponibles);
    }

    public RapportClientDetailDto obtenirDetailClient(Long utilisateurId, Long laboratoireId, Long rapportId) {
        Laboratoire laboratoire = exigerLaboratoire(laboratoireId);
        return tenantExecutor.inTenant(laboratoire.getNomSchema(), () -> {
            Rapport rapport = rapportRepository.findVisiblesClient(utilisateurId).stream()
                    .filter(r -> r.getId().equals(rapportId))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Rapport", "id", rapportId));
            List<RapportLigneClientDto> lignes = rapportLigneRepository.findByRapport_Id(rapportId).stream()
                    .map(this::toLigne)
                    .toList();
            String synthese = syntheseIaRepository
                    .findFirstByDemande_IdOrderByDateGenerationDesc(rapport.getDemande().getId())
                    .map(SyntheseIa::getContenu)
                    .orElse(null);
            return toDetail(rapport, laboratoire, lignes, synthese);
        });
    }

    private Laboratoire exigerLaboratoire(Long laboratoireId) {
        return tenantExecutor.inCentral(() -> laboratoireRepository.findById(laboratoireId)
                .orElseThrow(() -> new ResourceNotFoundException("Laboratoire", "id", laboratoireId)));
    }

    private RapportClientResumeDto toResume(Rapport rapport, Laboratoire laboratoire) {
        Demande demande = rapport.getDemande();
        return RapportClientResumeDto.builder()
                .rapportId(rapport.getId())
                .laboratoireId(laboratoire.getId())
                .laboratoireNom(laboratoire.getRaisonSociale())
                .demandeId(demande.getId())
                .demandeReference(demande.getNumero())
                .demandeTitre(demande.getTitre())
                .type(rapport.getType())
                .version(rapport.getVersion())
                .dateEmission(rapport.getDateEmission())
                .telechargeable(false)
                .build();
    }

    private RapportClientDetailDto toDetail(Rapport rapport, Laboratoire laboratoire,
                                            List<RapportLigneClientDto> lignes, String synthese) {
        Demande demande = rapport.getDemande();
        return RapportClientDetailDto.builder()
                .rapportId(rapport.getId())
                .laboratoireId(laboratoire.getId())
                .laboratoireNom(laboratoire.getRaisonSociale())
                .demandeId(demande.getId())
                .demandeReference(demande.getNumero())
                .demandeTitre(demande.getTitre())
                .type(rapport.getType())
                .version(rapport.getVersion())
                .dateEmission(rapport.getDateEmission())
                .dateSignature(rapport.getDateSignature())
                .telechargeable(false)
                .lignes(lignes)
                .syntheseIa(synthese)
                .mentionSyntheseIa(synthese == null ? null : MENTION_SYNTHESE_IA)
                .build();
    }

    private RapportLigneClientDto toLigne(RapportLigne ligne) {
        return RapportLigneClientDto.builder()
                .ligneEssaiId(ligne.getLigneEssai().getId())
                .designation(ligne.getLigneEssai().getEssai().getDesignation())
                .valeur(ligne.getLigneEssai().getValeur())
                .unite(ligne.getLigneEssai().getEssai().getUnite())
                .conformite(ligne.getLigneEssai().getConformite())
                .build();
    }
}
