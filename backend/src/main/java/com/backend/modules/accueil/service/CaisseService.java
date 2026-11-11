package com.backend.modules.accueil.service;

import com.backend.common.audit.JournalAuditService;
import com.backend.common.exception.BusinessRuleException;
import com.backend.common.exception.ResourceNotFoundException;
import com.backend.common.security.SecurityUtils;
import com.backend.common.tenant.TenantExecutor;
import com.backend.common.util.ApresCommit;
import com.backend.modules.accueil.dto.CaisseDtos.EncaissementDto;
import com.backend.modules.accueil.dto.CaisseDtos.EncaissementRequest;
import com.backend.modules.accueil.dto.CaisseDtos.FactureCaisseDto;
import com.backend.modules.accueil.dto.CaisseDtos.PaiementCaisseDto;
import com.backend.modules.accueil.repository.CaisseRepository;
import com.backend.modules.auth.security.UtilisateurPrincipal;
import com.backend.modules.demande.entity.Demande;
import com.backend.modules.facturation.entity.Facture;
import com.backend.modules.facturation.entity.Paiement;
import com.backend.modules.facturation.entity.StatutFacture;
import com.backend.modules.notification.service.NotificationService;
import com.backend.modules.plateforme.repository.LaboratoireRepository;
import com.backend.modules.planification.service.LigneTravailAssembleur;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Caisse de l'accueil (M11) : encaissement des factures, éventuellement en plusieurs fois,
 * et reçu de paiement numéroté {@code PAY-AAAAMM-NNN}. La facture passe à « payée » quand
 * le dernier règlement solde le montant.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaisseService {

    private static final ZoneId FUSEAU = ZoneId.of("Africa/Casablanca");
    private static final DateTimeFormatter MOIS = DateTimeFormatter.ofPattern("yyyyMM");
    private static final Set<String> MODES_AVEC_REFERENCE = Set.of("CHEQUE", "VIREMENT");

    private final CaisseRepository caisseRepository;
    private final LigneTravailAssembleur assembleur;
    private final LaboratoireRepository laboratoireRepository;
    private final TenantExecutor tenantExecutor;
    private final JournalAuditService journalAuditService;
    private final NotificationService notificationService;
    private final RecuPdfService recuPdfService;

    @Transactional(readOnly = true)
    public List<FactureCaisseDto> lister() {
        List<Facture> factures = caisseRepository.factures();
        return versDtos(factures);
    }

    @Transactional
    public EncaissementDto encaisser(Long factureId, EncaissementRequest request) {
        UtilisateurPrincipal acteur = SecurityUtils.principalCourant();
        Facture facture = caisseRepository.facture(factureId)
                .orElseThrow(() -> new ResourceNotFoundException("Facture", "id", factureId));
        List<Paiement> anciens = caisseRepository.paiementsDes(List.of(factureId));
        BigDecimal paye = somme(anciens);
        BigDecimal reste = facture.getMontantHt().subtract(paye).max(BigDecimal.ZERO);
        if (StatutFacture.ANNULEE.name().equals(facture.getStatut())) {
            throw new BusinessRuleException("FACTURE_ANNULEE", "La facture " + facture.getNumero()
                    + " est annulée par l'avoir " + facture.getNumeroAvoir() + " : rien n'est à encaisser.");
        }
        if (reste.signum() == 0 || StatutFacture.PAYEE.name().equals(facture.getStatut())) {
            throw new BusinessRuleException("FACTURE_DEJA_PAYEE", "La facture " + facture.getNumero() + " est déjà réglée.");
        }
        BigDecimal montant = request.montant().setScale(2, RoundingMode.HALF_UP);
        if (montant.compareTo(reste) > 0) {
            throw new BusinessRuleException("MONTANT_SUPERIEUR_AU_RESTE",
                    "Le montant dépasse le reste à payer (" + RecuPdfService.mad(reste) + ").");
        }
        String reference = StringUtils.hasText(request.reference()) ? request.reference().trim() : null;
        if (MODES_AVEC_REFERENCE.contains(request.mode()) && reference == null) {
            throw new BusinessRuleException("REFERENCE_OBLIGATOIRE",
                    "Indiquez le numéro du " + ("CHEQUE".equals(request.mode()) ? "chèque." : "virement."));
        }

        String prefixe = "PAY-" + LocalDate.now(FUSEAU).format(MOIS) + "-";
        Integer dernier = caisseRepository.dernierNumero(prefixe + "%");
        Paiement paiement = caisseRepository.save(Paiement.builder()
                .code(prefixe + String.format(Locale.ROOT, "%03d", (dernier == null ? 0 : dernier) + 1))
                .datePaiement(java.time.Instant.now())
                .montant(montant)
                .mode(request.mode())
                .reference(reference)
                .facture(facture)
                .encaisseurId(acteur.getId())
                .build());

        BigDecimal resteApres = reste.subtract(montant);
        String statutAvant = facture.getStatut();
        if (resteApres.signum() == 0) {
            facture.setStatut(StatutFacture.PAYEE.name());
        }
        journalAuditService.enregistrer("ENCAISSEMENT_FACTURE", "Facture", facture.getId(), acteur.getEmail(),
                "{\"statut\":\"" + statutAvant + "\",\"reste\":" + reste + "}",
                "{\"statut\":\"" + facture.getStatut() + "\",\"recu\":\"" + paiement.getCode() + "\",\"montant\":" + montant
                        + ",\"mode\":\"" + request.mode() + "\",\"reste\":" + resteApres + "}",
                null, acteur.getLaboratoireId());
        prevenirClient(facture, montant, resteApres, acteur.getLaboratoireId());

        FactureCaisseDto dto = versDtos(List.of(facture)).getFirst();
        PaiementCaisseDto paiementDto = dto.paiements().stream()
                .filter(p -> p.id().equals(paiement.getId())).findFirst().orElse(null);
        return new EncaissementDto(dto, paiementDto);
    }

    /** Reçu PDF d'un paiement : montant de la facture, déjà réglé avant, montant reçu et reste. */
    @Transactional(readOnly = true)
    public FichierRecu recu(Long paiementId) {
        Paiement paiement = caisseRepository.paiement(paiementId)
                .orElseThrow(() -> new ResourceNotFoundException("Paiement", "id", paiementId));
        Facture facture = paiement.getFacture();
        List<Paiement> tous = caisseRepository.paiementsDes(List.of(facture.getId()));
        // Déjà réglé avant ce paiement (ordre chronologique, puis identifiant).
        BigDecimal avant = tous.stream()
                .filter(p -> p.getDatePaiement().isBefore(paiement.getDatePaiement())
                        || (p.getDatePaiement().equals(paiement.getDatePaiement()) && p.getId() < paiement.getId()))
                .map(Paiement::getMontant).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal reste = facture.getMontantHt().subtract(avant).subtract(paiement.getMontant()).max(BigDecimal.ZERO);
        Demande demande = facture.getDemande();
        Map<Long, String> noms = assembleur.nomsUtilisateurs(Stream.of(paiement.getEncaisseurId(), clientUtilisateur(demande))
                .filter(Objects::nonNull).collect(Collectors.toSet()));
        byte[] pdf = recuPdfService.generer(new RecuPdfService.DonneesRecu(
                enTete(), paiement.getCode(), paiement.getDatePaiement(), nomClient(demande, noms),
                demande == null || demande.getClient() == null ? null : demande.getClient().getIce(),
                facture.getNumero(), facture.getDateFacture(), demande == null ? null : demande.getNumero(),
                facture.getMontantHt(), avant, paiement.getMontant(), paiement.getMode(), paiement.getReference(), reste,
                noms.get(paiement.getEncaisseurId())));
        return new FichierRecu("Recu-" + paiement.getCode() + ".pdf", pdf);
    }

    public record FichierRecu(String nom, byte[] contenu) {
    }

    // ------------------------------------------------------------------ assemblage

    private List<FactureCaisseDto> versDtos(List<Facture> factures) {
        if (factures.isEmpty()) {
            return List.of();
        }
        Map<Long, List<Paiement>> paiements = caisseRepository.paiementsDes(
                        factures.stream().map(Facture::getId).toList()).stream()
                .collect(Collectors.groupingBy(p -> p.getFacture().getId()));
        Set<Long> utilisateurs = new HashSet<>();
        factures.forEach(f -> {
            Long client = clientUtilisateur(f.getDemande());
            if (client != null) utilisateurs.add(client);
        });
        paiements.values().forEach(liste -> liste.forEach(p -> {
            if (p.getEncaisseurId() != null) utilisateurs.add(p.getEncaisseurId());
        }));
        Map<Long, String> noms = assembleur.nomsUtilisateurs(utilisateurs);
        LocalDate aujourdhui = LocalDate.now(FUSEAU);

        List<FactureCaisseDto> resultat = new ArrayList<>();
        for (Facture f : factures) {
            List<Paiement> siens = paiements.getOrDefault(f.getId(), List.of());
            BigDecimal paye = somme(siens);
            BigDecimal reste = f.getMontantHt().subtract(paye).max(BigDecimal.ZERO);
            // Une facture annulée (avoir) ou réglée n'a plus rien à encaisser.
            boolean payee = reste.signum() == 0 || StatutFacture.PAYEE.name().equals(f.getStatut())
                    || StatutFacture.ANNULEE.name().equals(f.getStatut());
            Demande d = f.getDemande();
            resultat.add(new FactureCaisseDto(f.getId(), f.getNumero(), f.getDateFacture(), f.getDateEcheance(),
                    f.getMontantHt(), paye, payee ? BigDecimal.ZERO : reste, f.getStatut(),
                    !payee && f.getDateEcheance() != null && f.getDateEcheance().isBefore(aujourdhui),
                    d == null ? null : d.getId(), d == null ? null : d.getNumero(), d == null ? null : d.getTitre(),
                    nomClient(d, noms), d == null || d.getClient() == null ? null : d.getClient().getIce(),
                    siens.isEmpty() ? null : siens.getFirst().getDatePaiement(),
                    siens.stream().map(p -> new PaiementCaisseDto(p.getId(), p.getCode(), p.getDatePaiement(),
                            p.getMontant(), p.getMode(), p.getReference(), noms.get(p.getEncaisseurId()))).toList()));
        }
        return resultat;
    }

    private static BigDecimal somme(List<Paiement> paiements) {
        return paiements.stream().map(Paiement::getMontant).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private RecuPdfService.EnTete enTete() {
        Long laboratoireId = SecurityUtils.principalCourant().getLaboratoireId();
        return tenantExecutor.inCentral(() -> laboratoireRepository.findById(laboratoireId)
                .map(l -> new RecuPdfService.EnTete(l.getRaisonSociale(),
                        joindre(", ", l.getAdresse(), joindre(" ", l.getCodePostal(), l.getVille())),
                        joindre(" · ", l.getTelephone(), l.getEmail()), l.getIce()))
                .orElse(new RecuPdfService.EnTete("Laboratoire", null, null, null)));
    }

    /** In-app au client : paiement reçu. Un échec n'annule jamais l'encaissement. */
    private void prevenirClient(Facture facture, BigDecimal montant, BigDecimal reste, Long laboratoireId) {
        Long clientId = clientUtilisateur(facture.getDemande());
        if (clientId == null) {
            return;
        }
        String corps = "Paiement de " + RecuPdfService.mad(montant) + " reçu pour la facture " + facture.getNumero()
                + (reste.signum() == 0 ? " : facture réglée." : ". Reste à payer : " + RecuPdfService.mad(reste) + ".");
        ApresCommit.executer(() -> {
            try {
                tenantExecutor.inCentral(() -> {
                    notificationService.notifierUtilisateurs(List.of(clientId), "PAIEMENT_RECU", "Paiement reçu",
                            corps, "/app/factures");
                    return null;
                });
            } catch (RuntimeException ex) {
                log.warn("Client non prévenu du paiement de la facture {} : {}", facture.getNumero(), ex.getMessage());
            }
        });
    }

    private static Long clientUtilisateur(Demande d) {
        return d == null || d.getClient() == null ? null : d.getClient().getUtilisateurId();
    }

    private static String nomClient(Demande d, Map<Long, String> noms) {
        if (d == null) {
            return null;
        }
        if (d.getClient() == null) {
            // Patient sur site, sans compte client : la facture est à son nom.
            return d.getPatient() == null ? null
                    : ("Patient " + (d.getPatient().getPrenom() == null ? "" : d.getPatient().getPrenom() + " ")
                            + d.getPatient().getNom()).trim();
        }
        if (StringUtils.hasText(d.getClient().getRaisonSociale())) {
            return d.getClient().getRaisonSociale();
        }
        return noms.get(d.getClient().getUtilisateurId());
    }

    private static String joindre(String separateur, String... morceaux) {
        String resultat = Stream.of(morceaux).filter(StringUtils::hasText).collect(Collectors.joining(separateur));
        return resultat.isEmpty() ? null : resultat;
    }
}
