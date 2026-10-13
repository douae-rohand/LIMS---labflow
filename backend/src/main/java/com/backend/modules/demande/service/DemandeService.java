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
import com.backend.common.dto.PageResponse;
import com.backend.modules.demande.dto.AnalyseDemandeeDto;
import com.backend.modules.demande.dto.ClientDemandeDto;
import com.backend.modules.demande.dto.DemandeDto;
import com.backend.modules.demande.dto.DemandeLaboratoireDto;
import com.backend.modules.demande.dto.DemandeResumeDto;
import com.backend.modules.demande.dto.DocumentDemandeDto;
import com.backend.modules.demande.dto.EchantillonDemandeDto;
import com.backend.modules.demande.dto.IndicateursDemandesDto;
import com.backend.modules.demande.dto.PatientDemandeDto;
import com.backend.modules.demande.entity.Demande;
import com.backend.modules.demande.entity.PieceJointe;
import com.backend.modules.demande.entity.StatutDemande;
import com.backend.modules.demande.repository.DemandeRepository;
import com.backend.modules.demande.repository.PieceJointeRepository;
import com.backend.modules.echantillon.entity.Echantillon;
import com.backend.modules.echantillon.repository.EchantillonRepository;
import com.backend.modules.essai.entity.Essai;
import com.backend.modules.essai.entity.LigneEssai;
import com.backend.modules.essai.repository.LigneEssaiRepository;
import com.backend.modules.patient.entity.Patient;
import com.backend.modules.utilisateur.entity.Utilisateur;
import com.backend.modules.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final LigneEssaiRepository ligneEssaiRepository;
    private final PieceJointeRepository pieceJointeRepository;
    private final EchantillonRepository echantillonRepository;
    private final UtilisateurRepository utilisateurRepository;

    private static final int MOTIF_MIN = 5;
    private static final int MOTIF_MAX = 2000;
    private static final int PAGE_SIZE_MAX = 50;

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
                    .statut(StatutDemande.NOUVEAU.name())
                    .client(client)
                    .build();
            return toDto(demandeRepository.save(demande), nomSchema);
        });
    }

    @Transactional
    public DemandeDto soumettre(Long id) {
        Demande demande = charger(id);
        verifierProprieteClient(demande);
        demande.setStatut(StatutDemande.NOUVEAU.name());
        demande.setDateSoumission(Instant.now());
        return toDto(demandeRepository.save(demande), TenantContext.getCurrentTenant());
    }

    @Transactional
    public DemandeDto changerStatut(Long id, StatutDemande statut) {
        if (statut == StatutDemande.ACCEPTEE) {
            return toDto(enregistrerAcceptation(id), TenantContext.getCurrentTenant());
        }
        if (statut == StatutDemande.REFUSEE) {
            throw new BusinessRuleException("MOTIF_OBLIGATOIRE",
                    "Le refus d'une demande exige un motif.");
        }
        Demande demande = charger(id);
        demande.setStatut(statut.name());
        return toDto(demandeRepository.save(demande), TenantContext.getCurrentTenant());
    }

    @Transactional(readOnly = true)
    public PageResponse<DemandeResumeDto> listerPourLaboratoire(String q, String statut, int page, int size) {
        String recherche = normaliserRecherche(q);
        String statutFiltre = normaliserStatutFiltre(statut);
        int pageSafe = Math.max(page, 0);
        int sizeSafe = size < 1 ? 20 : Math.min(size, PAGE_SIZE_MAX);
        Pageable pageable = PageRequest.of(pageSafe, sizeSafe,
                Sort.by(Sort.Direction.DESC, "dateSoumission"));
        Page<DemandeResumeDto> resultat = demandeRepository.rechercher(recherche, statutFiltre, pageable)
                .map(this::toResume);
        return PageResponse.from(resultat);
    }

    @Transactional(readOnly = true)
    public IndicateursDemandesDto indicateursLaboratoire() {
        long total = demandeRepository.count();
        return IndicateursDemandesDto.builder()
                .enAttente(demandeRepository.countByStatut(StatutDemande.NOUVEAU.name()))
                .acceptees(demandeRepository.countByStatut(StatutDemande.ACCEPTEE.name()))
                .refusees(demandeRepository.countByStatut(StatutDemande.REFUSEE.name()))
                .total(total)
                .build();
    }

    @Transactional(readOnly = true)
    public DemandeLaboratoireDto detailLaboratoire(Long id) {
        return construireDetail(charger(id));
    }

    @Transactional
    public DemandeLaboratoireDto accepter(Long id) {
        return construireDetail(enregistrerAcceptation(id));
    }

    @Transactional
    public DemandeLaboratoireDto refuser(Long id, String motif) {
        Demande demande = charger(id);
        exigerNouvelle(demande);
        appliquerDecision(demande, StatutDemande.REFUSEE, validerMotif(motif));
        return construireDetail(demandeRepository.save(demande));
    }

    private Demande enregistrerAcceptation(Long id) {
        Demande demande = charger(id);
        exigerNouvelle(demande);
        appliquerDecision(demande, StatutDemande.ACCEPTEE, null);
        return demandeRepository.save(demande);
    }

    private void exigerNouvelle(Demande demande) {
        if (!StatutDemande.NOUVEAU.name().equals(demande.getStatut())) {
            throw new BusinessRuleException("DEMANDE_DEJA_TRAITEE",
                    "Cette demande ne peut plus être acceptée ou refusée.");
        }
    }

    private void appliquerDecision(Demande demande, StatutDemande statut, String motif) {
        UtilisateurPrincipal principal = SecurityUtils.principalCourant();
        demande.setStatut(statut.name());
        demande.setDateDecision(Instant.now());
        demande.setDecideurId(principal.getId());
        demande.setMotif(motif);
    }

    private String validerMotif(String motif) {
        if (motif == null || motif.isBlank()) {
            throw new BusinessRuleException("MOTIF_OBLIGATOIRE",
                    "Le motif du refus est obligatoire.");
        }
        String propre = motif.trim();
        if (propre.length() < MOTIF_MIN) {
            throw new BusinessRuleException("MOTIF_TROP_COURT",
                    "Le motif doit contenir au moins 5 caractères.");
        }
        if (propre.length() > MOTIF_MAX) {
            throw new BusinessRuleException("MOTIF_TROP_LONG",
                    "Le motif ne peut pas dépasser 2000 caractères.");
        }
        return propre;
    }

    private String normaliserRecherche(String q) {
        if (q == null || q.isBlank()) {
            return "";
        }
        String trimmed = q.trim();
        if (trimmed.length() > 100) {
            throw new BusinessRuleException("RECHERCHE_TROP_LONGUE",
                    "La recherche ne peut pas dépasser 100 caractères.");
        }
        return trimmed;
    }

    private String normaliserStatutFiltre(String statut) {
        if (statut == null || statut.isBlank()) {
            return "";
        }
        try {
            return StatutDemande.valueOf(statut.trim()).name();
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("STATUT_INVALIDE", "Statut de demande inconnu.");
        }
    }

    private DemandeResumeDto toResume(Demande demande) {
        return DemandeResumeDto.builder()
                .id(demande.getId())
                .reference(demande.getNumero())
                .objet(demande.getTitre())
                .statut(enumOuNull(StatutDemande.class, demande.getStatut()))
                .demandeur(libelleDemandeur(demande))
                .dateSoumission(demande.getDateSoumission())
                .dateDecision(demande.getDateDecision())
                .decisionPossible(StatutDemande.NOUVEAU.name().equals(demande.getStatut()))
                .build();
    }

    private String libelleDemandeur(Demande demande) {
        if (demande.getClient() != null && demande.getClient().getRaisonSociale() != null
                && !demande.getClient().getRaisonSociale().isBlank()) {
            return demande.getClient().getRaisonSociale();
        }
        if (demande.getPatient() != null) {
            return nomComplet(demande.getPatient().getPrenom(), demande.getPatient().getNom());
        }
        return null;
    }

    private DemandeLaboratoireDto construireDetail(Demande demande) {
        Long id = demande.getId();
        List<LigneEssai> lignes = ligneEssaiRepository.findByDemande_Id(id);
        List<PieceJointe> pieces = pieceJointeRepository.findByDemande_Id(id);
        List<Echantillon> echantillons = echantillonRepository
                .findByDemande_Id(id, PageRequest.of(0, 50))
                .getContent();

        Client client = demande.getClient();
        Patient patient = demande.getPatient();
        for (LigneEssai ligne : lignes) {
            Essai essai = ligne.getEssai();
            if (essai != null) {
                essai.getDesignation();
            }
        }

        Set<Long> utilisateurIds = new HashSet<>();
        if (demande.getDecideurId() != null) {
            utilisateurIds.add(demande.getDecideurId());
        }
        if (client != null && client.getUtilisateurId() != null) {
            utilisateurIds.add(client.getUtilisateurId());
        }
        Map<Long, Utilisateur> utilisateurs = chargerUtilisateurs(utilisateurIds);
        Utilisateur contactClient = client == null || client.getUtilisateurId() == null
                ? null
                : utilisateurs.get(client.getUtilisateurId());
        Utilisateur decideur = demande.getDecideurId() == null
                ? null
                : utilisateurs.get(demande.getDecideurId());

        return DemandeLaboratoireDto.builder()
                .id(demande.getId())
                .reference(demande.getNumero())
                .objet(demande.getTitre())
                .objectif(demande.getObjectif())
                .statut(enumOuNull(StatutDemande.class, demande.getStatut()))
                .dateSoumission(demande.getDateSoumission())
                .dateDecision(demande.getDateDecision())
                .motif(demande.getMotif())
                .decideurId(demande.getDecideurId())
                .decideurNom(decideur == null ? null : nomComplet(decideur.getPrenom(), decideur.getNom()))
                .decisionPossible(StatutDemande.NOUVEAU.name().equals(demande.getStatut()))
                .client(client == null ? null : toClientDto(client, contactClient))
                .patient(patient == null ? null : toPatientDto(patient))
                .analyses(lignes.stream().map(this::toAnalyse).toList())
                .echantillons(echantillons.stream().map(this::toEchantillon).toList())
                .documents(pieces.stream().map(this::toDocument).toList())
                .build();
    }

    private Map<Long, Utilisateur> chargerUtilisateurs(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return tenantExecutor.inCentral(() -> {
            Map<Long, Utilisateur> map = new HashMap<>();
            utilisateurRepository.findAllById(ids).forEach(u -> map.put(u.getId(), u));
            return map;
        });
    }

    private ClientDemandeDto toClientDto(Client client, Utilisateur contact) {
        return ClientDemandeDto.builder()
                .id(client.getId())
                .code(client.getCode())
                .raisonSociale(client.getRaisonSociale())
                .ice(client.getIce())
                .adresse(client.getAdresse())
                .nomComplet(contact == null ? null : nomComplet(contact.getPrenom(), contact.getNom()))
                .email(contact == null ? null : contact.getEmail())
                .telephone(contact == null ? null : contact.getTelephone())
                .build();
    }

    private PatientDemandeDto toPatientDto(Patient patient) {
        return PatientDemandeDto.builder()
                .id(patient.getId())
                .code(patient.getCode())
                .nomComplet(nomComplet(patient.getPrenom(), patient.getNom()))
                .dateNaissance(patient.getDateNaissance())
                .telephone(patient.getTelephone())
                .email(patient.getEmail())
                .build();
    }

    private AnalyseDemandeeDto toAnalyse(LigneEssai ligne) {
        Essai essai = ligne.getEssai();
        return AnalyseDemandeeDto.builder()
                .id(ligne.getId())
                .code(essai == null ? ligne.getCode() : essai.getCode())
                .designation(essai == null ? null : essai.getDesignation())
                .methode(essai == null ? null : essai.getMethode())
                .unite(essai == null ? null : essai.getUnite())
                .tarif(essai == null ? null : essai.getTarif())
                .montant(ligne.getMontant())
                .statut(ligne.getStatut() == null ? null : ligne.getStatut().name())
                .build();
    }

    private EchantillonDemandeDto toEchantillon(Echantillon echantillon) {
        return EchantillonDemandeDto.builder()
                .id(echantillon.getId())
                .reference(echantillon.getReference())
                .nature(echantillon.getNature())
                .dateReception(echantillon.getDateReception())
                .conformite(echantillon.getConformite())
                .build();
    }

    private DocumentDemandeDto toDocument(PieceJointe piece) {
        return DocumentDemandeDto.builder()
                .id(piece.getId())
                .code(piece.getCode())
                .nomFichier(piece.getNomFichier())
                .typeMime(piece.getTypeMime())
                .taille(piece.getTaille())
                .dateAjout(piece.getDateAjout())
                .build();
    }

    private static String nomComplet(String prenom, String nom) {
        String gauche = prenom == null ? "" : prenom.trim();
        String droite = nom == null ? "" : nom.trim();
        String complet = (gauche + " " + droite).trim();
        return complet.isEmpty() ? null : complet;
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
