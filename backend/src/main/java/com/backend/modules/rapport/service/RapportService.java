package com.backend.modules.rapport.service;

import com.backend.common.audit.JournalAuditService;
import com.backend.common.exception.BusinessRuleException;
import com.backend.common.exception.ResourceNotFoundException;
import com.backend.common.security.ProprieteClient;
import com.backend.common.security.SecurityUtils;
import com.backend.common.tenant.TenantContext;
import com.backend.common.tenant.TenantExecutor;
import com.backend.common.util.ApresCommit;
import com.backend.integration.minio.StockageFichierService;
import com.backend.integration.sendgrid.EmailService;
import com.backend.modules.auth.security.UtilisateurPrincipal;
import com.backend.modules.demande.entity.Demande;
import com.backend.modules.demande.entity.StatutDemande;
import com.backend.modules.demande.repository.DemandeRepository;
import com.backend.modules.essai.entity.Essai;
import com.backend.modules.essai.entity.LigneEssai;
import com.backend.modules.essai.entity.StatutEssai;
import com.backend.modules.essai.repository.LigneEssaiRepository;
import com.backend.modules.plateforme.repository.LaboratoireRepository;
import com.backend.modules.planification.service.LigneTravailAssembleur;
import com.backend.modules.rapport.dto.RapportDto;
import com.backend.modules.rapport.dto.RapportTravailDto;
import com.backend.modules.rapport.entity.Rapport;
import com.backend.modules.rapport.entity.RapportLigne;
import com.backend.modules.rapport.entity.StatutRapport;
import com.backend.modules.rapport.repository.RapportLigneRepository;
import com.backend.modules.rapport.repository.RapportRepository;
import com.backend.modules.rapport.service.RapportPdfService.DonneesRapport;
import com.backend.modules.rapport.service.RapportPdfService.LigneRapport;
import com.backend.modules.utilisateur.entity.Utilisateur;
import com.backend.modules.utilisateur.repository.UtilisateurRepository;
import com.backend.modules.validation.entity.StatutValidation;
import com.backend.modules.validation.entity.Validation;
import com.backend.modules.validation.repository.ValidationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Rapports d'analyse : génération d'un projet à partir des résultats validés,
 * puis signature et libération par le responsable (PDF figé, empreinte SHA-256,
 * rapport visible du client et e-mail d'information).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RapportService {

    private final RapportRepository rapportRepository;
    private final RapportLigneRepository rapportLigneRepository;
    private final DemandeRepository demandeRepository;
    private final LigneEssaiRepository ligneEssaiRepository;
    private final ValidationRepository validationRepository;
    private final ProprieteClient proprieteClient;
    private final LigneTravailAssembleur assembleur;
    private final RapportPdfService rapportPdfService;
    private final StockageFichierService stockageFichierService;
    private final TenantExecutor tenantExecutor;
    private final LaboratoireRepository laboratoireRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final JournalAuditService journalAuditService;
    private final EmailService emailService;
    private final com.backend.modules.rapport.signature.SignatureNumeriqueService signatureNumeriqueService;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final com.backend.common.tenant.TenantResolver tenantResolver;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Transactional(readOnly = true)
    public RapportDto trouverParId(Long id) {
        if (!proprieteClient.clientConnecte()) {
            return toDto(charger(id));
        }
        Rapport rapport = rapportRepository.findById(id).orElseThrow(proprieteClient::introuvable);
        proprieteClient.exiger(rapport.getDemande());
        return toDto(rapport);
    }

    @Transactional(readOnly = true)
    public Page<RapportDto> listerParDemande(Long demandeId, Pageable pageable) {
        proprieteClient.exigerDemande(demandeId);
        return rapportRepository.findByDemande_Id(demandeId, pageable).map(this::toDto);
    }

    /** Demandes terminées et leur dernier rapport : à générer, à signer ou libéré. */
    @Transactional(readOnly = true)
    public List<RapportTravailDto> travail() {
        List<Demande> demandes = new ArrayList<>();
        for (String statut : StatutDemande.TERMINEE.aliasesPersistés()) {
            demandes.addAll(demandeRepository.findByStatut(statut, Pageable.unpaged()).getContent());
        }
        if (demandes.isEmpty()) {
            return List.of();
        }
        Map<Long, Rapport> derniers = new HashMap<>();
        for (Rapport rapport : rapportRepository.findByDemande_IdInOrderByIdDesc(
                demandes.stream().map(Demande::getId).toList())) {
            derniers.putIfAbsent(rapport.getDemande().getId(), rapport);
        }
        Set<Long> utilisateurs = new HashSet<>();
        demandes.forEach(d -> {
            if (d.getClient() != null && d.getClient().getUtilisateurId() != null) {
                utilisateurs.add(d.getClient().getUtilisateurId());
            }
        });
        derniers.values().forEach(r -> {
            if (r.getSignataireId() != null) utilisateurs.add(r.getSignataireId());
        });
        Map<Long, String> noms = assembleur.nomsUtilisateurs(utilisateurs);
        return demandes.stream()
                .sorted(Comparator.comparing(Demande::getDateSoumission, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(d -> {
                    Rapport rapport = derniers.get(d.getId());
                    return new RapportTravailDto(
                            d.getId(),
                            d.getNumero(),
                            d.getTitre(),
                            nomClient(d, noms),
                            ligneEssaiRepository.findByDemande_Id(d.getId()).stream()
                                    .filter(l -> l.getStatut() != StatutEssai.NON_REALISABLE).count(),
                            rapport == null ? null : rapport.getId(),
                            rapport == null ? null : rapport.getNumero(),
                            rapport == null ? null : enumOuNull(rapport.getStatut()),
                            rapport == null ? null : rapport.getDateEmission(),
                            rapport == null ? null : rapport.getDateSignature(),
                            rapport == null || rapport.getSignataireId() == null ? null : noms.get(rapport.getSignataireId()),
                            rapport == null ? null : empreinteDepuisCle(rapport.getClePdf()));
                })
                .toList();
    }

    /**
     * Crée le projet de rapport d'une demande terminée dont toutes les analyses réalisables
     * ont été validées par le responsable. Un projet non signé existant est réutilisé.
     */
    @Transactional
    public RapportDto generer(Long demandeId) {
        UtilisateurPrincipal acteur = SecurityUtils.principalCourant();
        Demande demande = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande", "id", demandeId));
        if (StatutDemande.depuis(demande.getStatut()) != StatutDemande.TERMINEE) {
            throw new BusinessRuleException("DEMANDE_NON_TERMINEE",
                    "Le rapport ne peut être généré qu'une fois tous les résultats validés.");
        }
        List<LigneEssai> lignes = lignesValidees(demande);
        List<Rapport> existants = rapportRepository.findByDemande_IdInOrderByIdDesc(List.of(demandeId));
        Rapport dernier = existants.isEmpty() ? null : existants.getFirst();
        if (dernier != null && StatutRapport.GENERE.name().equals(dernier.getStatut())) {
            return toDto(dernier);
        }
        int version = existants.size() + 1;
        Rapport rapport = rapportRepository.save(Rapport.builder()
                .numero("RAP-" + demande.getNumero() + (version > 1 ? "-V" + version : ""))
                .type("ANALYSE")
                .version((short) version)
                .demande(demande)
                .statut(StatutRapport.GENERE.name())
                .dateEmission(Instant.now())
                .diffuse(false)
                .build());
        for (LigneEssai ligne : lignes) {
            rapportLigneRepository.save(RapportLigne.builder().rapport(rapport).ligneEssai(ligne).build());
        }
        journalAuditService.enregistrer("GENERER_RAPPORT", "Rapport", rapport.getId(), acteur.getEmail(),
                null, "{\"numero\":\"" + rapport.getNumero() + "\"}", null, acteur.getLaboratoireId());
        return toDto(rapport);
    }

    /** Aperçu PDF du projet, marqué « non signé ». */
    @Transactional(readOnly = true)
    public byte[] apercu(Long rapportId) {
        Rapport rapport = charger(rapportId);
        return rapportPdfService.generer(donnees(rapport, laboratoireCourant(), null, null, true, null));
    }

    /**
     * Signe et libère le rapport : PDF définitif conservé avec son empreinte SHA-256
     * dans le nom de fichier, statut ENVOYE, visible du client, qui est prévenu par e-mail.
     */
    @Transactional
    public RapportTravailDto signerEtLiberer(Long rapportId, String motDePasse) {
        UtilisateurPrincipal acteur = SecurityUtils.principalCourant();
        confirmerIdentite(acteur, motDePasse);
        Rapport rapport = charger(rapportId);
        if (!StatutRapport.GENERE.name().equals(rapport.getStatut())) {
            throw new BusinessRuleException("RAPPORT_DEJA_SIGNE", "Ce rapport est déjà signé.");
        }
        Instant maintenant = Instant.now();
        String schema = TenantContext.getCurrentTenant() == null ? "laboratoire" : TenantContext.getCurrentTenant();
        LaboratoireInfo labo = laboratoireCourant();
        String urlVerification = frontendUrl.replaceAll("/$", "") + "/verifier-rapport?laboratoire="
                + java.net.URLEncoder.encode(labo.code(), java.nio.charset.StandardCharsets.UTF_8)
                + "&numero=" + java.net.URLEncoder.encode(rapport.getNumero(), java.nio.charset.StandardCharsets.UTF_8);
        byte[] brouillon = rapportPdfService.generer(
                donnees(rapport, labo, acteur.getNomComplet(), maintenant, false, urlVerification));
        com.backend.modules.rapport.signature.SignatureNumeriqueService.PdfSigne signe = signatureNumeriqueService.signer(
                brouillon, schema, labo.pdf().nom(), acteur.getNomComplet(), labo.ville(), maintenant);
        byte[] pdf = signe.contenu();
        String empreinte = sha256(pdf);
        String cle = "rapports/" + schema + "/" + rapport.getNumero() + "_" + empreinte + ".pdf";
        try (InputStream contenu = new ByteArrayInputStream(pdf)) {
            stockageFichierService.televerser(cle, contenu, pdf.length, "application/pdf");
        } catch (IOException ex) {
            throw new BusinessRuleException("RAPPORT_STOCKAGE_ECHOUE", "Le PDF signé n'a pas pu être enregistré.");
        }
        rapport.setClePdf(cle);
        rapport.setSignataireId(acteur.getId());
        rapport.setDateSignature(maintenant);
        rapport.setStatut(StatutRapport.ENVOYE.name());
        rapport.setDiffuse(true);
        rapportRepository.save(rapport);
        journalAuditService.enregistrer("SIGNER_LIBERER_RAPPORT", "Rapport", rapport.getId(), acteur.getEmail(),
                "{\"statut\":\"GENERE\"}", "{\"statut\":\"ENVOYE\",\"empreinte\":\"" + empreinte
                        + "\",\"horodate\":" + (signe.horodatage() != null) + "}",
                null, acteur.getLaboratoireId());

        Demande demande = rapport.getDemande();
        Long clientId = demande.getClient() == null ? null : demande.getClient().getUtilisateurId();
        String numero = rapport.getNumero();
        String lien = frontendUrl.replaceAll("/$", "") + "/app/rapports";
        ApresCommit.executer(() -> {
            if (clientId == null) {
                return;
            }
            try {
                Utilisateur client = tenantExecutor.inCentral(() -> utilisateurRepository.findById(clientId).orElse(null));
                if (client != null && StringUtils.hasText(client.getEmail())) {
                    emailService.envoyerRapportDisponible(client.getEmail(), nomComplet(client), numero, lien);
                }
            } catch (Exception ex) {
                log.error("E-mail « rapport disponible » non envoyé pour {} : {}", numero, ex.getMessage());
            }
        });
        Map<Long, String> noms = assembleur.nomsUtilisateurs(
                clientId == null ? List.of(acteur.getId()) : List.of(acteur.getId(), clientId));
        return new RapportTravailDto(demande.getId(), demande.getNumero(), demande.getTitre(), nomClient(demande, noms),
                rapportLigneRepository.findByRapport_Id(rapport.getId()).size(), rapport.getId(), numero,
                StatutRapport.ENVOYE, rapport.getDateEmission(), maintenant, acteur.getNomComplet(), empreinte);
    }

    /**
     * PDF signé conservé. Un rapport signé sans fichier enregistré (données de démonstration,
     * rapports antérieurs à la signature PDF) est recomposé à partir de son signataire
     * et de sa date de signature.
     */
    @Transactional(readOnly = true)
    public FichierRapport telecharger(Long rapportId) {
        Rapport rapport = charger(rapportId);
        String nom = rapport.getNumero() + ".pdf";
        if (StringUtils.hasText(rapport.getClePdf())) {
            try (InputStream contenu = stockageFichierService.telecharger(rapport.getClePdf())) {
                return new FichierRapport(nom, contenu.readAllBytes());
            } catch (BusinessRuleException | IOException ex) {
                log.warn("PDF stocké indisponible pour {}, recomposition : {}", rapport.getNumero(), ex.getMessage());
            }
        }
        if (StatutRapport.GENERE.name().equals(rapport.getStatut())
                || StatutRapport.BROUILLON.name().equals(rapport.getStatut())) {
            throw new BusinessRuleException("RAPPORT_NON_SIGNE", "Ce rapport n'est pas encore signé.");
        }
        String signataire = rapport.getSignataireId() == null ? null
                : assembleur.nomsUtilisateurs(List.of(rapport.getSignataireId())).get(rapport.getSignataireId());
        Instant dateSignature = rapport.getDateSignature() != null ? rapport.getDateSignature() : rapport.getDateEmission();
        return new FichierRapport(nom, rapportPdfService.generer(
                donnees(rapport, laboratoireCourant(), signataire,
                        dateSignature == null ? Instant.now() : dateSignature, false, null)));
    }

    public record FichierRapport(String nom, byte[] contenu) {
    }

    /** Ce que la page publique de vérification affiche : aucune donnée de patient ni résultat. */
    public record VerificationPublique(
            String laboratoire,
            String numero,
            String statut,
            Instant dateSignature,
            String signataire,
            /** Renseignés seulement quand un fichier a été déposé. */
            Boolean fichierAuthentique,
            Boolean signatureValide,
            Boolean documentComplet,
            Instant horodatage,
            String certificat) {
    }

    /** Statut public d'un rapport, à partir du QR code (code du laboratoire + numéro). */
    public VerificationPublique verifierPublic(String codeLaboratoire, String numero, byte[] fichier) {
        com.backend.modules.plateforme.entity.Laboratoire labo = tenantResolver.exigerActif(codeLaboratoire);
        return tenantExecutor.inTenant(labo.getNomSchema(), () -> {
            Rapport rapport = rapportRepository.findByNumero(numero)
                    .filter(r -> Boolean.TRUE.equals(r.getDiffuse()))
                    .orElseThrow(() -> new BusinessRuleException("RAPPORT_INCONNU",
                            "Aucun rapport signé ne porte ce numéro dans ce laboratoire."));
            String signataire = rapport.getSignataireId() == null ? null
                    : assembleur.nomsUtilisateurs(List.of(rapport.getSignataireId())).get(rapport.getSignataireId());
            if (fichier == null) {
                return new VerificationPublique(labo.getRaisonSociale(), rapport.getNumero(), rapport.getStatut(),
                        rapport.getDateSignature(), signataire, null, null, null, null, null);
            }
            String attendue = empreinteDepuisCle(rapport.getClePdf());
            boolean authentique = attendue != null && attendue.equals(sha256(fichier));
            var verification = signatureNumeriqueService.verifier(fichier);
            return new VerificationPublique(labo.getRaisonSociale(), rapport.getNumero(), rapport.getStatut(),
                    rapport.getDateSignature(), signataire, authentique,
                    verification.signaturePresente() && verification.signatureValide(),
                    verification.documentComplet(), verification.horodatage(), verification.certificat());
        });
    }

    // -------------------------------------------------------------------------

    private List<LigneEssai> lignesValidees(Demande demande) {
        List<LigneEssai> lignes = ligneEssaiRepository.findByDemande_Id(demande.getId()).stream()
                .filter(l -> l.getStatut() != StatutEssai.NON_REALISABLE)
                .toList();
        List<LigneEssai> nonValidees = lignes.stream()
                .filter(l -> !LigneTravailAssembleur.VALIDEE.equals(LigneTravailAssembleur.etatValidation(
                        validationRepository.findByLigneEssai_IdOrderByDateValidationAscIdAsc(l.getId()))))
                .toList();
        if (lignes.isEmpty() || !nonValidees.isEmpty()) {
            throw new BusinessRuleException("RESULTATS_NON_VALIDES",
                    "Toutes les analyses doivent être validées par le responsable avant de générer le rapport.");
        }
        return lignes;
    }

    private record LaboratoireInfo(RapportPdfService.Laboratoire pdf, String code, String ville) {
    }

    private LaboratoireInfo laboratoireCourant() {
        UtilisateurPrincipal acteur = SecurityUtils.principalCourant();
        return tenantExecutor.inCentral(() -> laboratoireRepository
                .findById(acteur.getLaboratoireId())
                .map(l -> new LaboratoireInfo(new RapportPdfService.Laboratoire(
                        l.getRaisonSociale(),
                        joindre(", ", l.getAdresse(), joindre(" ", l.getCodePostal(), l.getVille())),
                        joindre(" · ", l.getTelephone(), l.getEmail()),
                        l.getIce()), l.getCode(), l.getVille()))
                .orElse(new LaboratoireInfo(new RapportPdfService.Laboratoire("Laboratoire", null, null, null),
                        "laboratoire", null)));
    }

    /** La signature exige le mot de passe du responsable : preuve qu'il signe lui-même. */
    private void confirmerIdentite(UtilisateurPrincipal acteur, String motDePasse) {
        if (!StringUtils.hasText(motDePasse)) {
            throw new BusinessRuleException("CONFIRMATION_REQUISE", "Saisissez votre mot de passe pour signer.");
        }
        String hash = tenantExecutor.inCentral(() -> utilisateurRepository.findById(acteur.getId())
                .map(Utilisateur::getMotDePasseHash).orElse(null));
        if (hash == null || !passwordEncoder.matches(motDePasse, hash)) {
            log.warn("Signature refusée : mot de passe incorrect pour userId={}", acteur.getId());
            throw new BusinessRuleException("CONFIRMATION_INCORRECTE", "Mot de passe incorrect : rapport non signé.");
        }
    }

    private DonneesRapport donnees(Rapport rapport, LaboratoireInfo laboratoire, String signataire,
                                   Instant dateSignature, boolean projet, String urlVerification) {
        Demande demande = rapport.getDemande();
        RapportPdfService.Laboratoire labo = laboratoire.pdf();

        List<LigneEssai> lignes = rapportLigneRepository.findByRapport_Id(rapport.getId()).stream()
                .map(RapportLigne::getLigneEssai)
                .sorted(Comparator.comparing(LigneEssai::getId))
                .toList();
        Map<Long, Validation> decisions = new HashMap<>();
        for (LigneEssai ligne : lignes) {
            validationRepository.findByLigneEssai_IdOrderByDateValidationAscIdAsc(ligne.getId()).stream()
                    .filter(v -> v.getNiveau() != null && v.getNiveau() >= 2
                            && StatutValidation.VALIDEE.name().equals(v.getDecision()))
                    .reduce((a, b) -> b)
                    .ifPresent(v -> decisions.put(ligne.getId(), v));
        }
        Set<Long> utilisateurs = decisions.values().stream().map(Validation::getValidateurId).collect(Collectors.toSet());
        if (demande.getClient() != null && demande.getClient().getUtilisateurId() != null) {
            utilisateurs.add(demande.getClient().getUtilisateurId());
        }
        Map<Long, String> noms = assembleur.nomsUtilisateurs(utilisateurs);
        List<LigneRapport> lignesRapport = lignes.stream().map(ligne -> {
            Essai essai = ligne.getEssai();
            Validation decision = decisions.get(ligne.getId());
            return new LigneRapport(
                    essai == null ? ligne.getCode() : essai.getDesignation(),
                    essai == null ? null : essai.getMethode(),
                    ligne.getValeur(),
                    essai == null ? null : essai.getUnite(),
                    essai == null ? null : reference(essai.getLimiteMin(), essai.getLimiteMax()),
                    ligne.getConformite() == null ? "—" : (ligne.getConformite() ? "Conforme" : "Non conforme"),
                    decision == null ? null : noms.get(decision.getValidateurId()));
        }).toList();
        return new DonneesRapport(labo, rapport.getNumero(),
                rapport.getVersion() == null ? 1 : rapport.getVersion(),
                demande.getNumero(), demande.getTitre(), nomClient(demande, noms),
                rapport.getDateEmission(), lignesRapport, signataire, dateSignature, projet, urlVerification);
    }

    private static String reference(BigDecimal min, BigDecimal max) {
        if (min == null && max == null) return null;
        if (min == null) return "≤ " + max.stripTrailingZeros().toPlainString();
        if (max == null) return "≥ " + min.stripTrailingZeros().toPlainString();
        return min.stripTrailingZeros().toPlainString() + " – " + max.stripTrailingZeros().toPlainString();
    }

    private static String nomClient(Demande demande, Map<Long, String> noms) {
        if (demande.getClient() == null) {
            return null;
        }
        if (StringUtils.hasText(demande.getClient().getRaisonSociale())) {
            return demande.getClient().getRaisonSociale();
        }
        return noms.get(demande.getClient().getUtilisateurId());
    }

    private static String empreinteDepuisCle(String cle) {
        if (!StringUtils.hasText(cle) || !cle.contains("_") || !cle.endsWith(".pdf")) {
            return null;
        }
        return cle.substring(cle.lastIndexOf('_') + 1, cle.length() - 4);
    }

    private static String sha256(byte[] contenu) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contenu));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 non disponible", ex);
        }
    }

    private static String joindre(String separateur, String... valeurs) {
        String resultat = Arrays.stream(valeurs).filter(StringUtils::hasText).collect(Collectors.joining(separateur));
        return resultat.isEmpty() ? null : resultat;
    }

    private static String nomComplet(Utilisateur u) {
        String prenom = u.getPrenom() == null ? "" : u.getPrenom().trim();
        return (prenom + " " + (u.getNom() == null ? "" : u.getNom().trim())).trim();
    }

    private Rapport charger(Long id) {
        return rapportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rapport", "id", id));
    }

    private static StatutRapport enumOuNull(String valeur) {
        if (valeur == null) {
            return null;
        }
        try {
            return StatutRapport.valueOf(valeur);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private RapportDto toDto(Rapport rapport) {
        return RapportDto.builder()
                .id(rapport.getId())
                .reference(rapport.getNumero())
                .demandeId(rapport.getDemande() == null ? null : rapport.getDemande().getId())
                .statut(enumOuNull(rapport.getStatut()))
                .fichierUrl(rapport.getClePdf())
                .signataireId(rapport.getSignataireId())
                .dateGeneration(rapport.getDateEmission())
                .dateEnvoi(Boolean.TRUE.equals(rapport.getDiffuse()) ? rapport.getDateSignature() : null)
                .build();
    }
}
