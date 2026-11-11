package com.backend.modules.accueil.service;

import com.backend.common.audit.JournalAuditService;
import com.backend.common.exception.BusinessRuleException;
import com.backend.common.exception.ResourceNotFoundException;
import com.backend.common.security.SecurityUtils;
import com.backend.common.tenant.TenantExecutor;
import com.backend.common.util.ApresCommit;
import com.backend.integration.sendgrid.EmailService;
import com.backend.modules.accueil.dto.ReceptionDtos.DemandeReceptionDto;
import com.backend.modules.accueil.dto.ReceptionDtos.DetailReceptionDto;
import com.backend.modules.accueil.dto.ReceptionDtos.EchantillonRecuDto;
import com.backend.modules.accueil.dto.ReceptionDtos.EchantillonRecuRequest;
import com.backend.modules.accueil.dto.ReceptionDtos.EtatReception;
import com.backend.modules.accueil.dto.ReceptionDtos.ExigenceDto;
import com.backend.modules.accueil.dto.ReceptionDtos.LigneReceptionDto;
import com.backend.modules.accueil.dto.ReceptionDtos.NatureAttendueDto;
import com.backend.modules.accueil.dto.ReceptionDtos.ReceptionRequest;
import com.backend.modules.accueil.repository.ReceptionRepository;
import com.backend.modules.auth.security.UtilisateurPrincipal;
import com.backend.modules.demande.entity.Demande;
import com.backend.modules.demande.entity.StatutDemande;
import com.backend.modules.echantillon.entity.Echantillon;
import com.backend.modules.essai.entity.ExigenceEchantillon;
import com.backend.modules.essai.entity.LigneEssai;
import com.backend.modules.essai.entity.StatutEssai;
import com.backend.modules.essai.repository.ExigenceEchantillonRepository;
import com.backend.modules.notification.service.NotificationService;
import com.backend.modules.planification.service.LigneTravailAssembleur;
import com.backend.modules.utilisateur.entity.RoleUtilisateur;
import com.backend.modules.utilisateur.entity.Utilisateur;
import com.backend.modules.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Réception des échantillons par l'accueil (M03, étape 3 du workflow).
 *
 * <p>Chaque échantillon reçu reçoit une référence unique {@code ECH-AAAAMM-NNN} et est
 * rattaché aux analyses qu'il permet de réaliser : c'est ce lien qui autorise le technicien
 * à démarrer l'essai. Un échantillon non conforme exige un motif ; ses analyses restent
 * en attente d'un nouvel échantillon. La réception peut être partielle.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReceptionService {

    /**
     * Une demande reste recevable tant que son rapport n'est pas signé : en réception partielle,
     * elle peut déjà être « en validation » alors que des échantillons manquent encore.
     * Seules les analyses sans échantillon conforme peuvent être couvertes (voir {@code aRecevoir}).
     */
    static final List<String> STATUTS_RECEVABLES = List.of(StatutDemande.ACCEPTEE.name(), StatutDemande.EN_COURS.name(),
            StatutDemande.EN_VALIDATION.name());
    private static final ZoneId FUSEAU = ZoneId.of("Africa/Casablanca");
    private static final DateTimeFormatter MOIS = DateTimeFormatter.ofPattern("yyyyMM");

    private final ReceptionRepository receptionRepository;
    private final ExigenceEchantillonRepository exigenceEchantillonRepository;
    private final LigneTravailAssembleur assembleur;
    private final UtilisateurRepository utilisateurRepository;
    private final TenantExecutor tenantExecutor;
    private final JournalAuditService journalAuditService;
    private final NotificationService notificationService;
    private final EmailService emailService;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    // ------------------------------------------------------------------ lecture

    /** Demandes acceptées ou en cours, les plus récemment actives en premier. */
    @Transactional(readOnly = true)
    public List<DemandeReceptionDto> lister() {
        List<LigneEssai> lignes = receptionRepository.lignesDesDemandes(STATUTS_RECEVABLES);
        return resumer(lignes);
    }

    @Transactional(readOnly = true)
    public DetailReceptionDto detail(Long demandeId) {
        List<LigneEssai> lignes = receptionRepository.lignesDeLaDemande(demandeId);
        if (lignes.isEmpty()) {
            throw new ResourceNotFoundException("Demande", "id", demandeId);
        }
        List<Echantillon> echantillons = receptionRepository.echantillonsDeLaDemande(demandeId);
        Map<Long, List<ExigenceEchantillon>> exigences = exigencesDes(lignes);
        Set<Long> utilisateurs = new HashSet<>();
        lignes.forEach(l -> ajouter(utilisateurs, l.getTechnicienId(), clientUtilisateur(l.getDemande())));
        echantillons.forEach(e -> ajouter(utilisateurs, e.getReceptionneurId()));
        Map<Long, String> noms = assembleur.nomsUtilisateurs(utilisateurs);

        List<LigneReceptionDto> lignesDto = lignes.stream().map(l -> new LigneReceptionDto(
                l.getId(), l.getCode(),
                l.getEssai() == null ? null : l.getEssai().getCode(),
                l.getEssai() == null ? null : l.getEssai().getDesignation(),
                l.getEssai() == null || l.getEssai().getDomaine() == null ? null : l.getEssai().getDomaine().getLibelle(),
                l.getTechnicienId() == null ? null : noms.get(l.getTechnicienId()),
                l.getStatut() == null ? null : l.getStatut().name(),
                aRecevoir(l),
                l.getEchantillon() == null ? null : l.getEchantillon().getReference(),
                l.getEchantillon() == null ? null : !Boolean.FALSE.equals(l.getEchantillon().getConformite()),
                exigences.getOrDefault(essaiId(l), List.of()).stream()
                        .map(x -> new ExigenceDto(x.getNature(), x.getEtatConservation(), x.getQuantiteMinimale(), x.getUnite()))
                        .toList()
        )).toList();

        Map<Long, List<String>> analysesParEchantillon = new HashMap<>();
        lignes.stream().filter(l -> l.getEchantillon() != null).forEach(l -> analysesParEchantillon
                .computeIfAbsent(l.getEchantillon().getId(), k -> new ArrayList<>()).add(libelle(l)));
        List<EchantillonRecuDto> echantillonsDto = echantillons.stream()
                .map(e -> versDto(e, noms, analysesParEchantillon.getOrDefault(e.getId(), List.of())))
                .toList();

        return new DetailReceptionDto(resumer(lignes).getFirst(), lignesDto, naturesAttendues(lignes, exigences),
                echantillonsDto);
    }

    // ------------------------------------------------------------------ réception

    @Transactional
    public DetailReceptionDto receptionner(Long demandeId, ReceptionRequest request) {
        UtilisateurPrincipal acteur = SecurityUtils.principalCourant();
        List<LigneEssai> lignes = receptionRepository.lignesDeLaDemande(demandeId);
        if (lignes.isEmpty()) {
            throw new ResourceNotFoundException("Demande", "id", demandeId);
        }
        Demande demande = lignes.getFirst().getDemande();
        if (!STATUTS_RECEVABLES.contains(demande.getStatut())) {
            throw new BusinessRuleException("DEMANDE_NON_RECEVABLE",
                    "Les échantillons ne peuvent être reçus que pour une demande acceptée, en cours ou en validation.");
        }
        Map<Long, LigneEssai> parId = lignes.stream().collect(Collectors.toMap(LigneEssai::getId, l -> l));
        Set<Long> dejaCouvertes = new HashSet<>();
        for (EchantillonRecuRequest recu : request.echantillons()) {
            if (!recu.conforme() && !StringUtils.hasText(recu.motif())) {
                throw new BusinessRuleException("MOTIF_OBLIGATOIRE",
                        "Indiquez le motif de non-conformité de l'échantillon « " + recu.nature().trim() + " ».");
            }
            for (Long ligneId : recu.ligneIds()) {
                LigneEssai ligne = parId.get(ligneId);
                if (ligne == null) {
                    throw new BusinessRuleException("ANALYSE_HORS_DEMANDE",
                            "Une analyse choisie n'appartient pas à la demande " + demande.getNumero() + ".");
                }
                if (!aRecevoir(ligne)) {
                    throw new BusinessRuleException("ANALYSE_DEJA_COUVERTE",
                            "L'analyse " + libelle(ligne) + " a déjà un échantillon conforme ou a démarré.");
                }
                if (!dejaCouvertes.add(ligneId)) {
                    throw new BusinessRuleException("ANALYSE_EN_DOUBLE",
                            "L'analyse " + libelle(ligne) + " est rattachée à deux échantillons.");
                }
            }
        }

        Instant maintenant = Instant.now();
        String prefixe = "ECH-" + LocalDate.now(FUSEAU).format(MOIS) + "-";
        Integer dernier = receptionRepository.dernierNumero(prefixe + "%");
        int numero = dernier == null ? 0 : dernier;
        List<String> nonConformes = new ArrayList<>();
        Set<Long> techniciens = new HashSet<>();
        int recus = 0;
        for (EchantillonRecuRequest recu : request.echantillons()) {
            numero++;
            Echantillon echantillon = receptionRepository.save(Echantillon.builder()
                    .reference(prefixe + String.format(Locale.ROOT, "%03d", numero))
                    .nature(recu.nature().trim())
                    .dateReception(maintenant)
                    .conformite(recu.conforme())
                    .motif(StringUtils.hasText(recu.motif()) ? recu.motif().trim() : null)
                    .etatConservationConstate(StringUtils.hasText(recu.etatConservationConstate())
                            ? recu.etatConservationConstate().trim() : null)
                    .demande(demande)
                    .receptionneurId(acteur.getId())
                    .build());
            List<String> codes = new ArrayList<>();
            for (Long ligneId : recu.ligneIds()) {
                LigneEssai ligne = parId.get(ligneId);
                ligne.setEchantillon(echantillon);
                // Les analyses créées depuis le panier n'ont pas encore de statut : celui attendu par
                // le technicien pour « Démarrer » est ATTENTE_ECHANTILLON.
                if (ligne.getStatut() == null) {
                    ligne.setStatut(StatutEssai.ATTENTE_ECHANTILLON);
                }
                codes.add(ligne.getCode());
                if (recu.conforme() && ligne.getTechnicienId() != null) {
                    techniciens.add(ligne.getTechnicienId());
                }
            }
            recus++;
            if (!recu.conforme()) {
                nonConformes.add(echantillon.getNature() + " (" + echantillon.getReference() + ") : " + echantillon.getMotif());
            }
            journalAuditService.enregistrer("RECEPTIONNER_ECHANTILLON", "Echantillon", echantillon.getId(),
                    acteur.getEmail(), null,
                    "{\"reference\":\"" + echantillon.getReference() + "\",\"demande\":\"" + demande.getNumero()
                            + "\",\"conforme\":" + recu.conforme() + ",\"analyses\":\"" + String.join(",", codes) + "\"}",
                    echantillon.getMotif(), acteur.getLaboratoireId());
        }
        prevenir(demande, acteur, recus, nonConformes, techniciens);
        return detail(demandeId);
    }

    // ------------------------------------------------------------------ calculs partagés

    /** Une analyse attend un échantillon si elle n'a pas démarré et n'a pas d'échantillon conforme. */
    static boolean aRecevoir(LigneEssai ligne) {
        StatutEssai statut = ligne.getStatut();
        boolean enAttente = statut == null || statut == StatutEssai.ATTENTE_ECHANTILLON;
        return enAttente && (ligne.getEchantillon() == null || Boolean.FALSE.equals(ligne.getEchantillon().getConformite()));
    }

    static boolean realisable(LigneEssai ligne) {
        return ligne.getStatut() != StatutEssai.NON_REALISABLE;
    }

    List<DemandeReceptionDto> resumer(List<LigneEssai> lignes) {
        Map<Long, List<LigneEssai>> parDemande = new LinkedHashMap<>();
        lignes.forEach(l -> parDemande.computeIfAbsent(l.getDemande().getId(), k -> new ArrayList<>()).add(l));
        if (parDemande.isEmpty()) {
            return List.of();
        }
        Map<Long, Instant> dernieres = new HashMap<>();
        for (Object[] ligne : receptionRepository.dernieresReceptions(parDemande.keySet())) {
            dernieres.put((Long) ligne[0], (Instant) ligne[1]);
        }
        Map<Long, String> noms = assembleur.nomsUtilisateurs(parDemande.values().stream()
                .map(l -> clientUtilisateur(l.getFirst().getDemande())).filter(Objects::nonNull).collect(Collectors.toSet()));
        return parDemande.values().stream()
                .map(siennes -> resume(siennes, noms, dernieres))
                .sorted(Comparator.comparing(ReceptionService::activite, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private static DemandeReceptionDto resume(List<LigneEssai> siennes, Map<Long, String> noms, Map<Long, Instant> dernieres) {
        Demande d = siennes.getFirst().getDemande();
        List<LigneEssai> realisables = siennes.stream().filter(ReceptionService::realisable).toList();
        int enAttente = (int) realisables.stream().filter(ReceptionService::aRecevoir).count();
        int recues = realisables.size() - enAttente;
        int nonConformes = (int) realisables.stream()
                .filter(l -> l.getEchantillon() != null && Boolean.FALSE.equals(l.getEchantillon().getConformite()))
                .count();
        EtatReception etat = enAttente == 0 ? EtatReception.COMPLETE
                : recues > 0 || nonConformes > 0 ? EtatReception.PARTIELLE : EtatReception.A_RECEVOIR;
        return new DemandeReceptionDto(d.getId(), d.getNumero(), d.getTitre(), nomClient(d, noms), nomPatient(d),
                d.getStatut(), d.getDateDecision(), dernieres.get(d.getId()), realisables.size(), enAttente, recues,
                nonConformes, etat);
    }

    private static Instant activite(DemandeReceptionDto d) {
        return Stream.of(d.derniereReception(), d.dateDecision())
                .filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null);
    }

    EchantillonRecuDto versDto(Echantillon e, Map<Long, String> noms, List<String> analyses) {
        Demande d = e.getDemande();
        return new EchantillonRecuDto(e.getId(), e.getReference(), e.getNature(), e.getDateReception(),
                !Boolean.FALSE.equals(e.getConformite()), e.getMotif(), e.getEtatConservationConstate(),
                noms.get(e.getReceptionneurId()), d == null ? null : d.getId(), d == null ? null : d.getNumero(), analyses);
    }

    private Map<Long, List<ExigenceEchantillon>> exigencesDes(List<LigneEssai> lignes) {
        Set<Long> essais = lignes.stream().map(ReceptionService::essaiId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (essais.isEmpty()) {
            return Map.of();
        }
        return exigenceEchantillonRepository.findByEssai_IdIn(essais).stream()
                .collect(Collectors.groupingBy(x -> x.getEssai().getId()));
    }

    /** Regroupe par nature les échantillons encore attendus (analyses sans exigence : « Non précisée »). */
    private static List<NatureAttendueDto> naturesAttendues(List<LigneEssai> lignes,
                                                            Map<Long, List<ExigenceEchantillon>> exigences) {
        record Groupe(String nature, Set<String> conservations, Set<String> quantites, List<Long> lignes) {
        }
        Map<String, Groupe> groupes = new LinkedHashMap<>();
        for (LigneEssai ligne : lignes) {
            if (!realisable(ligne) || !aRecevoir(ligne)) {
                continue;
            }
            List<ExigenceEchantillon> siennes = exigences.getOrDefault(essaiId(ligne), List.of());
            if (siennes.isEmpty()) {
                groupes.computeIfAbsent("", k -> new Groupe(null, new HashSet<>(), new HashSet<>(), new ArrayList<>()))
                        .lignes().add(ligne.getId());
                continue;
            }
            for (ExigenceEchantillon exigence : siennes) {
                String cle = exigence.getNature().trim().toLowerCase(Locale.ROOT);
                Groupe groupe = groupes.computeIfAbsent(cle, k -> new Groupe(exigence.getNature().trim(),
                        new HashSet<>(), new HashSet<>(), new ArrayList<>()));
                if (StringUtils.hasText(exigence.getEtatConservation())) {
                    groupe.conservations().add(exigence.getEtatConservation().trim());
                }
                if (exigence.getQuantiteMinimale() != null) {
                    groupe.quantites().add(exigence.getQuantiteMinimale().stripTrailingZeros().toPlainString()
                            + (exigence.getUnite() == null ? "" : " " + exigence.getUnite()));
                }
                if (!groupe.lignes().contains(ligne.getId())) {
                    groupe.lignes().add(ligne.getId());
                }
            }
        }
        return groupes.values().stream()
                .map(g -> new NatureAttendueDto(g.nature(),
                        g.conservations().isEmpty() ? null : String.join(" · ", g.conservations()),
                        g.quantites().isEmpty() ? null : "min. " + String.join(" / ", g.quantites()),
                        g.lignes()))
                .toList();
    }

    /** Client (e-mail + in-app), techniciens affectés (in-app) et, en cas de non-conformité, responsables. */
    private void prevenir(Demande demande, UtilisateurPrincipal acteur, int recus, List<String> nonConformes,
                          Set<Long> techniciens) {
        Long clientId = clientUtilisateur(demande);
        Long laboratoireId = acteur.getLaboratoireId();
        String numero = demande.getNumero();
        Long demandeId = demande.getId();
        String base = frontendUrl.replaceAll("/$", "");
        ApresCommit.executer(() -> {
            String lienClient = "/app/demandes/" + laboratoireId + "/" + demandeId;
            String resume = recus + " échantillon(s) reçu(s) pour la demande " + numero
                    + (nonConformes.isEmpty() ? "." : ", dont " + nonConformes.size() + " non conforme(s).");
            if (clientId != null) {
                notifier(List.of(clientId), "ECHANTILLON_RECU", "Échantillons reçus", resume, lienClient);
                try {
                    Utilisateur client = tenantExecutor.inCentral(() -> utilisateurRepository.findById(clientId).orElse(null));
                    if (client != null && StringUtils.hasText(client.getEmail())) {
                        String nom = ((client.getPrenom() == null ? "" : client.getPrenom() + " ")
                                + (client.getNom() == null ? "" : client.getNom())).trim();
                        emailService.envoyerEchantillonsRecus(client.getEmail(), nom, numero, recus, nonConformes,
                                base + lienClient);
                    }
                } catch (RuntimeException ex) {
                    log.warn("E-mail de réception non envoyé pour la demande {} : {}", numero, ex.getMessage());
                }
            }
            notifier(techniciens, "ECHANTILLON_RECU", "Échantillon reçu",
                    "L'échantillon de la demande " + numero + " est arrivé : vous pouvez démarrer vos analyses.",
                    "/app/essais");
            if (!nonConformes.isEmpty() && laboratoireId != null) {
                try {
                    List<Long> responsables = tenantExecutor.inCentral(() -> utilisateurRepository
                            .findActifsParRoleEtLaboratoire(RoleUtilisateur.RESPONSABLE.name(), laboratoireId)
                            .stream().map(Utilisateur::getId).toList());
                    notifier(responsables, "ECHANTILLON_NON_CONFORME", "Échantillon non conforme",
                            String.join(" ; ", nonConformes), "/app/suivi");
                } catch (RuntimeException ex) {
                    log.warn("Responsables non prévenus pour la demande {} : {}", numero, ex.getMessage());
                }
            }
        });
    }

    /**
     * Notification in-app dans le schéma central (les comptes y sont stockés). Appelée après
     * validation de la réception : un échec est journalisé sans jamais annuler la réception.
     */
    private void notifier(Collection<Long> destinataires, String type, String titre, String corps, String lien) {
        if (destinataires == null || destinataires.isEmpty()) {
            return;
        }
        try {
            tenantExecutor.inCentral(() -> {
                notificationService.notifierUtilisateurs(destinataires, type, titre, corps, lien);
                return null;
            });
        } catch (RuntimeException ex) {
            log.warn("Notification « {} » non envoyée : {}", titre, ex.getMessage());
        }
    }

    private static String nomClient(Demande d, Map<Long, String> noms) {
        if (d.getClient() == null) {
            return null;
        }
        if (StringUtils.hasText(d.getClient().getRaisonSociale())) {
            return d.getClient().getRaisonSociale();
        }
        return noms.get(d.getClient().getUtilisateurId());
    }

    private static String nomPatient(Demande d) {
        if (d.getPatient() == null) {
            return null;
        }
        String nom = ((d.getPatient().getPrenom() == null ? "" : d.getPatient().getPrenom() + " ")
                + (d.getPatient().getNom() == null ? "" : d.getPatient().getNom())).trim();
        return nom.isEmpty() ? null : nom;
    }

    private static Long clientUtilisateur(Demande d) {
        return d == null || d.getClient() == null ? null : d.getClient().getUtilisateurId();
    }

    private static Long essaiId(LigneEssai l) {
        return l.getEssai() == null ? null : l.getEssai().getId();
    }

    static String libelle(LigneEssai l) {
        String essai = l.getEssai() == null ? null : l.getEssai().getDesignation();
        return essai == null ? l.getCode() : l.getCode() + " (" + essai + ")";
    }

    private static void ajouter(Collection<Long> cible, Long... ids) {
        for (Long id : ids) {
            if (id != null) {
                cible.add(id);
            }
        }
    }
}
