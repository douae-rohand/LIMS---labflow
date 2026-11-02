package com.backend.modules.satisfaction.service;

import com.backend.common.audit.JournalAudit;
import com.backend.common.audit.JournalAuditService;
import com.backend.common.dto.ListeAgregeeDto;
import com.backend.common.exception.BusinessRuleException;
import com.backend.common.exception.ResourceNotFoundException;
import com.backend.common.security.SecurityUtils;
import com.backend.common.tenant.TenantExecutor;
import com.backend.modules.auth.security.UtilisateurPrincipal;
import com.backend.modules.client.entity.ClientLaboratoire;
import com.backend.modules.client.repository.ClientLaboratoireRepository;
import com.backend.modules.demande.entity.Demande;
import com.backend.modules.plateforme.entity.Laboratoire;
import com.backend.modules.plateforme.repository.LaboratoireRepository;
import com.backend.modules.satisfaction.dto.EnqueteClientDetailDto;
import com.backend.modules.satisfaction.dto.EnqueteClientResumeDto;
import com.backend.modules.satisfaction.dto.RepondreEnqueteRequest;
import com.backend.modules.satisfaction.entity.EnqueteSatisfaction;
import com.backend.modules.satisfaction.repository.EnqueteSatisfactionRepository;
import com.backend.modules.utilisateur.entity.Utilisateur;
import com.backend.modules.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Enquêtes de satisfaction de l'espace client.
 *
 * <p>La réponse est unique et atomique ; l'audit {@code REPONSE_ENQUETE} est
 * écrit après le retour de la transaction d'écriture, avec un acteur résolu
 * dans le thread appelant et sans donnée personnelle ni commentaire.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SatisfactionClientService {

    private static final String ACTION_REPONSE_ENQUETE = "REPONSE_ENQUETE";

    private final EnqueteSatisfactionRepository enqueteRepository;
    private final LaboratoireRepository laboratoireRepository;
    private final ClientLaboratoireRepository clientLaboratoireRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final JournalAuditService journalAuditService;
    private final TenantExecutor tenantExecutor;

    public ListeAgregeeDto<EnqueteClientResumeDto> listerEnquetesClient(Long utilisateurId) {
        List<ClientLaboratoire> liaisons = tenantExecutor.inCentral(
                () -> clientLaboratoireRepository.findByUtilisateur_Id(utilisateurId));
        List<EnqueteClientResumeDto> resultats = new ArrayList<>();
        int laboratoiresIndisponibles = 0;
        for (ClientLaboratoire liaison : liaisons) {
            Laboratoire laboratoire = liaison.getLaboratoire();
            try {
                List<EnqueteClientResumeDto> duLaboratoire = tenantExecutor.inTenant(laboratoire.getNomSchema(),
                        () -> enqueteRepository.findParUtilisateurAvecDemande(utilisateurId).stream()
                                .map(e -> toResume(e, laboratoire))
                                .toList());
                resultats.addAll(duLaboratoire);
            } catch (Exception ex) {
                laboratoiresIndisponibles++;
                log.warn("Enquêtes indisponibles pour le laboratoire {} : {}",
                        laboratoire.getCode(), ex.getMessage());
            }
        }
        // Non répondues d'abord (false avant true), puis date d'envoi décroissante.
        resultats.sort(Comparator.comparing(EnqueteClientResumeDto::isRepondue)
                .thenComparing(EnqueteClientResumeDto::getDateEnvoi,
                        Comparator.nullsLast(Comparator.reverseOrder())));
        return ListeAgregeeDto.de(resultats, laboratoiresIndisponibles);
    }

    public EnqueteClientDetailDto obtenirDetailClient(Long utilisateurId, Long laboratoireId, Long enqueteId) {
        Laboratoire laboratoire = exigerLaboratoire(laboratoireId);
        return tenantExecutor.inTenant(laboratoire.getNomSchema(), () ->
                enqueteRepository.findVisibleClientParId(enqueteId, utilisateurId)
                        .map(e -> toDetail(e, laboratoire))
                        .orElseThrow(() -> new ResourceNotFoundException("EnqueteSatisfaction", "id", enqueteId)));
    }

    public EnqueteClientDetailDto repondre(Long utilisateurId, Long laboratoireId, Long enqueteId,
                                           RepondreEnqueteRequest request) {
        Laboratoire laboratoire = exigerLaboratoire(laboratoireId);
        String nomSchema = laboratoire.getNomSchema();
        String commentaire = normaliserCommentaire(request.getCommentaire());

        // Appartenance au client : 404 générique sinon (jamais 403).
        tenantExecutor.inTenant(nomSchema, () -> enqueteRepository.findVisibleClientParId(enqueteId, utilisateurId)
                .orElseThrow(() -> new ResourceNotFoundException("EnqueteSatisfaction", "id", enqueteId))
                .getId());

        Instant dateReponse = Instant.now();
        int lignes = tenantExecutor.inTenant(nomSchema, () -> enqueteRepository.repondreSiNonRepondue(
                enqueteId, dateReponse, request.getNoteGlobale(), request.getNoteDelai(),
                request.getNoteClarte(), request.getNoteRelation(), commentaire));
        if (lignes == 0) {
            throw new BusinessRuleException("ENQUETE_DEJA_REPONDUE",
                    "Vous avez déjà répondu à cette enquête.");
        }

        enregistrerAuditReponse(nomSchema, enqueteId, request.getNoteGlobale());
        return obtenirDetailClient(utilisateurId, laboratoireId, enqueteId);
    }

    private Laboratoire exigerLaboratoire(Long laboratoireId) {
        return tenantExecutor.inCentral(() -> laboratoireRepository.findById(laboratoireId)
                .orElseThrow(() -> new ResourceNotFoundException("Laboratoire", "id", laboratoireId)));
    }

    /**
     * Écrit après le retour de {@code inTenant}, dans le thread appelant :
     * la méthode appelante n'est pas transactionnelle, l'audit est donc persisté
     * via son propre service (REQUIRES_NEW, asynchrone).
     */
    private void enregistrerAuditReponse(String nomSchema, Long enqueteId, Integer noteGlobale) {
        UtilisateurPrincipal principal = SecurityUtils.principalCourant();
        Utilisateur auteur = tenantExecutor.inCentral(() ->
                utilisateurRepository.findByEmail(principal.getEmail()).orElse(null));
        if (auteur == null) {
            log.warn("Audit {} ignoré : utilisateur introuvable pour {}", ACTION_REPONSE_ENQUETE,
                    principal.getEmail());
            return;
        }
        JournalAudit entree = JournalAudit.builder()
                .code("AUD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .dateAction(Instant.now())
                .action(ACTION_REPONSE_ENQUETE)
                .objet(nomSchema + "/EnqueteSatisfaction#" + enqueteId)
                .valeurApres("{\"noteGlobale\":" + noteGlobale + "}")
                .utilisateur(auteur)
                .build();
        journalAuditService.enregistrer(entree);
    }

    private static String normaliserCommentaire(String commentaire) {
        if (commentaire == null) {
            return null;
        }
        String propre = commentaire.trim();
        return propre.isEmpty() ? null : propre;
    }

    private EnqueteClientResumeDto toResume(EnqueteSatisfaction enquete, Laboratoire laboratoire) {
        Demande demande = enquete.getDemande();
        return EnqueteClientResumeDto.builder()
                .enqueteId(enquete.getId())
                .laboratoireId(laboratoire.getId())
                .laboratoireNom(laboratoire.getRaisonSociale())
                .demandeReference(demande.getNumero())
                .demandeTitre(demande.getTitre())
                .dateEnvoi(enquete.getDateEnvoi())
                .repondue(enquete.getDateReponse() != null)
                .dateReponse(enquete.getDateReponse())
                .noteGlobale(enquete.getNoteGlobale())
                .build();
    }

    private EnqueteClientDetailDto toDetail(EnqueteSatisfaction enquete, Laboratoire laboratoire) {
        Demande demande = enquete.getDemande();
        return EnqueteClientDetailDto.builder()
                .enqueteId(enquete.getId())
                .laboratoireId(laboratoire.getId())
                .laboratoireNom(laboratoire.getRaisonSociale())
                .demandeId(demande.getId())
                .demandeReference(demande.getNumero())
                .demandeTitre(demande.getTitre())
                .dateEnvoi(enquete.getDateEnvoi())
                .repondue(enquete.getDateReponse() != null)
                .dateReponse(enquete.getDateReponse())
                .noteGlobale(enquete.getNoteGlobale())
                .noteDelai(enquete.getNoteDelai())
                .noteClarte(enquete.getNoteClarte())
                .noteRelation(enquete.getNoteRelation())
                .commentaire(enquete.getCommentaire())
                .build();
    }
}
