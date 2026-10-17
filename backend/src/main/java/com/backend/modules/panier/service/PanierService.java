package com.backend.modules.panier.service;

import com.backend.common.exception.BusinessRuleException;
import com.backend.common.exception.ResourceNotFoundException;
import com.backend.common.tenant.TenantExecutor;
import com.backend.modules.essai.entity.Essai;
import com.backend.modules.essai.repository.EssaiRepository;
import com.backend.modules.panier.dto.LignePanierDto;
import com.backend.modules.panier.dto.PanierDetailDto;
import com.backend.modules.panier.dto.PanierResumeDto;
import com.backend.modules.panier.entity.LignePanier;
import com.backend.modules.panier.entity.Panier;
import com.backend.modules.panier.repository.LignePanierRepository;
import com.backend.modules.panier.repository.PanierRepository;
import com.backend.modules.plateforme.entity.Laboratoire;
import com.backend.modules.plateforme.repository.LaboratoireRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Gestion du panier client stocké en base centrale (lims_central).
 *
 * <p>Toutes les lectures/écritures centrales passent par {@link TenantExecutor#inCentral},
 * et la lecture des essais se fait dans le schéma du laboratoire via
 * {@link TenantExecutor#inTenant}. Chaque appel ouvre sa propre transaction
 * (REQUIRES_NEW) : le mapping des essais et du domaine (LAZY) se fait donc
 * intégralement DANS le supplier de inTenant, jamais après sa fermeture.
 */
@Service
@RequiredArgsConstructor
public class PanierService {

    /** Statut actif d'un laboratoire (colonne laboratoire.statut, type String). */
    static final String STATUT_LABORATOIRE_ACTIF = "ACTIF";

    private static final int LIMITE_LIGNES_PANIER = 50;

    private final PanierRepository panierRepository;
    private final LignePanierRepository lignePanierRepository;
    private final LaboratoireRepository laboratoireRepository;
    private final EssaiRepository essaiRepository;
    private final TenantExecutor tenantExecutor;

    // ------------------------------------------------------------------
    // GET /api/client/paniers
    // ------------------------------------------------------------------

    /**
     * Liste tous les paniers du client, groupés par laboratoire.
     *
     * <p>Requêtes SQL : 1 (paniers) + 1 (lignes de tous les paniers) + 1 (laboratoires)
     * + N (essais par laboratoire, côté tenant).
     */
    public List<PanierResumeDto> listerPaniers(Long utilisateurId) {
        List<PanierEssais> donnees = tenantExecutor.inCentral(() -> {
            List<Panier> paniers = panierRepository.findByUtilisateurId(utilisateurId);
            if (paniers.isEmpty()) {
                return List.<PanierEssais>of();
            }
            Set<Long> panierIds = paniers.stream().map(Panier::getId).collect(Collectors.toSet());
            List<LignePanier> lignes = lignePanierRepository.findByPanierIdIn(panierIds);
            Map<Long, List<Long>> essaisParPanier = new HashMap<>();
            for (LignePanier l : lignes) {
                essaisParPanier
                        .computeIfAbsent(l.getPanier().getId(), k -> new ArrayList<>())
                        .add(l.getEssaiId());
            }
            return paniers.stream()
                    .map(p -> new PanierEssais(
                            p.getId(),
                            p.getLaboratoireId(),
                            p.getDateModification(),
                            essaisParPanier.getOrDefault(p.getId(), List.of())))
                    .toList();
        });

        if (donnees.isEmpty()) {
            return List.of();
        }

        Set<Long> laboIds = donnees.stream().map(PanierEssais::laboratoireId).collect(Collectors.toSet());
        Map<Long, Laboratoire> labos = tenantExecutor.inCentral(() ->
                laboratoireRepository.findAllById(laboIds).stream()
                        .collect(Collectors.toMap(Laboratoire::getId, l -> l)));

        List<PanierResumeDto> result = new ArrayList<>();
        for (PanierEssais pl : donnees) {
            Laboratoire labo = labos.get(pl.laboratoireId());
            if (labo == null) {
                continue;
            }
            List<LignePanierDto> lignesDto = pl.essaiIds().isEmpty()
                    ? List.of()
                    : construireLignesDto(labo.getNomSchema(), pl.essaiIds());
            BigDecimal sousTotal = sousTotalDisponible(lignesDto);
            result.add(PanierResumeDto.builder()
                    .laboratoireId(pl.laboratoireId())
                    .laboratoireNom(labo.getRaisonSociale())
                    .nombreEssais(pl.essaiIds().size())
                    .sousTotal(sousTotal)
                    .dateModification(pl.dateModification())
                    .build());
        }
        return result;
    }

    // ------------------------------------------------------------------
    // GET /api/client/paniers/{laboratoireId}
    // ------------------------------------------------------------------

    /**
     * Retourne le panier d'un laboratoire. Si aucun panier n'existe, retourne
     * un panier vide (200). Si le laboratoire est inconnu, 404.
     */
    public PanierDetailDto obtenirPanier(Long utilisateurId, Long laboratoireId) {
        Laboratoire labo = exigerLaboratoireExistant(laboratoireId);

        Optional<Panier> panierOpt = tenantExecutor.inCentral(() ->
                panierRepository.findByUtilisateurIdAndLaboratoireId(utilisateurId, laboratoireId));

        if (panierOpt.isEmpty()) {
            return detailVide(labo);
        }

        List<Long> essaiIds = tenantExecutor.inCentral(() ->
                lignePanierRepository.findByPanierId(panierOpt.get().getId()).stream()
                        .map(LignePanier::getEssaiId)
                        .toList());

        List<LignePanierDto> lignesDto = essaiIds.isEmpty()
                ? List.of()
                : construireLignesDto(labo.getNomSchema(), essaiIds);

        return PanierDetailDto.builder()
                .laboratoireId(labo.getId())
                .laboratoireNom(labo.getRaisonSociale())
                .lignes(lignesDto)
                .sousTotal(sousTotalDisponible(lignesDto))
                .dateModification(panierOpt.get().getDateModification())
                .build();
    }

    // ------------------------------------------------------------------
    // POST /api/client/paniers/{laboratoireId}/essais
    // ------------------------------------------------------------------

    /**
     * Ajoute un essai au panier. Idempotent : si l'essai est déjà présent,
     * retourne l'état actuel (200). En cas de conflit d'unicité concurrent,
     * relit le panier dans une nouvelle transaction et retourne son état.
     */
    public PanierDetailDto ajouterEssai(Long utilisateurId, Long laboratoireId, Long essaiId) {
        Laboratoire labo = exigerLaboratoireActif(laboratoireId);
        String nomSchema = labo.getNomSchema();

        // Validation de l'essai dans le schéma du laboratoire
        tenantExecutor.inTenant(nomSchema, () -> {
            Essai e = essaiRepository.findById(essaiId)
                    .orElseThrow(() -> new BusinessRuleException(
                            "ESSAI_INDISPONIBLE", "Essai introuvable dans ce laboratoire."));
            if (!Boolean.TRUE.equals(e.getActif())) {
                throw new BusinessRuleException(
                        "ESSAI_INDISPONIBLE", "L'essai n'est pas actif.");
            }
            if (e.getTarif() == null) {
                throw new BusinessRuleException(
                        "ESSAI_INDISPONIBLE", "L'essai n'a pas de tarif.");
            }
            return null;
        });

        // Écriture centrale atomique (check 50 lignes + insertion + date_modification)
        Long panierId;
        try {
            panierId = tenantExecutor.inCentral(() -> ecrireAjout(utilisateurId, laboratoireId, essaiId));
        } catch (DataIntegrityViolationException ex) {
            // Conflit unique concurrent : la transaction est rollbackée.
            // Relecture dans une NOUVELLE transaction.
            panierId = tenantExecutor.inCentral(() ->
                    panierRepository.findByUtilisateurIdAndLaboratoireId(utilisateurId, laboratoireId)
                            .map(Panier::getId)
                            .orElse(null));
        }

        if (panierId == null) {
            return detailVide(labo);
        }
        return construireDetail(labo, panierId);
    }

    private Long ecrireAjout(Long utilisateurId, Long laboratoireId, Long essaiId) {
        Panier panier = panierRepository.findByUtilisateurIdAndLaboratoireId(utilisateurId, laboratoireId)
                .orElseGet(() -> panierRepository.save(Panier.builder()
                        .utilisateurId(utilisateurId)
                        .laboratoireId(laboratoireId)
                        .dateCreation(Instant.now())
                        .dateModification(Instant.now())
                        .build()));

        if (lignePanierRepository.countByPanierId(panier.getId()) >= LIMITE_LIGNES_PANIER) {
            throw new BusinessRuleException(
                    "PANIER_PLEIN",
                    "Le panier atteint la limite de " + LIMITE_LIGNES_PANIER + " essais.");
        }

        if (lignePanierRepository.findByPanierIdAndEssaiId(panier.getId(), essaiId).isPresent()) {
            return panier.getId(); // déjà présent : idempotent
        }

        lignePanierRepository.save(LignePanier.builder()
                .panier(panier)
                .essaiId(essaiId)
                .dateAjout(Instant.now())
                .build());

        panier.setDateModification(Instant.now());
        panierRepository.save(panier);
        return panier.getId();
    }

    // ------------------------------------------------------------------
    // DELETE /api/client/paniers/{laboratoireId}/essais/{essaiId}
    // ------------------------------------------------------------------

    /**
     * Retire un essai du panier. Si c'était le dernier, supprime le panier
     * (la FK ON DELETE CASCADE supprime les lignes). Retourne toujours 200
     * avec un PanierDetailDto (vide si le panier a été supprimé).
     */
    public PanierDetailDto retirerEssai(Long utilisateurId, Long laboratoireId, Long essaiId) {
        Laboratoire labo = exigerLaboratoireExistant(laboratoireId);

        Optional<Panier> panierOpt = tenantExecutor.inCentral(() ->
                panierRepository.findByUtilisateurIdAndLaboratoireId(utilisateurId, laboratoireId));

        if (panierOpt.isEmpty()) {
            return detailVide(labo);
        }

        Long panierId = panierOpt.get().getId();

        tenantExecutor.inCentral(() -> {
            lignePanierRepository.deleteByPanierIdAndEssaiId(panierId, essaiId);
            if (lignePanierRepository.countByPanierId(panierId) == 0) {
                panierRepository.deleteById(panierId);
            } else {
                panierRepository.findById(panierId).ifPresent(p -> {
                    p.setDateModification(Instant.now());
                    panierRepository.save(p);
                });
            }
            return null;
        });

        boolean encoreExistant = tenantExecutor.inCentral(() -> panierRepository.existsById(panierId));
        if (!encoreExistant) {
            return detailVide(labo);
        }
        return construireDetail(labo, panierId);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Laboratoire exigerLaboratoireExistant(Long laboratoireId) {
        return tenantExecutor.inCentral(() ->
                laboratoireRepository.findById(laboratoireId)
                        .orElseThrow(() -> new ResourceNotFoundException("Laboratoire", "id", laboratoireId)));
    }

    private Laboratoire exigerLaboratoireActif(Long laboratoireId) {
        return tenantExecutor.inCentral(() -> {
            Laboratoire l = laboratoireRepository
                    .findByIdAndStatut(laboratoireId, STATUT_LABORATOIRE_ACTIF)
                    .orElse(null);
            if (l != null) {
                return l;
            }
            if (laboratoireRepository.existsById(laboratoireId)) {
                throw new BusinessRuleException(
                        "LABORATOIRE_INDISPONIBLE", "Le laboratoire n'est pas actif.");
            }
            throw new ResourceNotFoundException("Laboratoire", "id", laboratoireId);
        });
    }

    /**
     * Charge les essais dans le schéma tenant et construit les DTO.
     * Le mapping (essai + domaine LAZY) se fait intégralement DANS le supplier
     * de inTenant pour éviter tout LazyInitializationException.
     */
    private List<LignePanierDto> construireLignesDto(String nomSchema, List<Long> essaiIds) {
        return tenantExecutor.inTenant(nomSchema, () -> {
            Map<Long, Essai> map = essaiRepository.findByIdsAvecDomaine(essaiIds).stream()
                    .collect(Collectors.toMap(Essai::getId, e -> e, (a, b) -> a));
            return essaiIds.stream().map(id -> {
                Essai e = map.get(id);
                if (e == null || !Boolean.TRUE.equals(e.getActif()) || e.getTarif() == null) {
                    return LignePanierDto.builder()
                            .essaiId(id)
                            .disponible(false)
                            .build();
                }
                return LignePanierDto.builder()
                        .essaiId(id)
                        .code(e.getCode())
                        .designation(e.getDesignation())
                        .domaineLibelle(e.getDomaine() != null ? e.getDomaine().getLibelle() : null)
                        .tarif(e.getTarif())
                        .dureeEstimee(e.getDureeEstimee())
                        .disponible(true)
                        .build();
            }).toList();
        });
    }

    private PanierDetailDto construireDetail(Laboratoire labo, Long panierId) {
        List<Long> essaiIds = tenantExecutor.inCentral(() ->
                lignePanierRepository.findByPanierId(panierId).stream()
                        .map(LignePanier::getEssaiId)
                        .toList());

        List<LignePanierDto> lignesDto = essaiIds.isEmpty()
                ? List.of()
                : construireLignesDto(labo.getNomSchema(), essaiIds);

        Instant dateModif = tenantExecutor.inCentral(() ->
                panierRepository.findById(panierId).map(Panier::getDateModification).orElse(null));

        return PanierDetailDto.builder()
                .laboratoireId(labo.getId())
                .laboratoireNom(labo.getRaisonSociale())
                .lignes(lignesDto)
                .sousTotal(sousTotalDisponible(lignesDto))
                .dateModification(dateModif)
                .build();
    }

    private PanierDetailDto detailVide(Laboratoire labo) {
        return PanierDetailDto.builder()
                .laboratoireId(labo.getId())
                .laboratoireNom(labo.getRaisonSociale())
                .lignes(List.of())
                .sousTotal(BigDecimal.ZERO)
                .dateModification(null)
                .build();
    }

    private BigDecimal sousTotalDisponible(List<LignePanierDto> lignes) {
        return lignes.stream()
                .filter(LignePanierDto::isDisponible)
                .map(LignePanierDto::getTarif)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Record interne pour transporter les données d'un panier hors de la
     * transaction centrale sans exposer d'entités détachées.
     */
    private record PanierEssais(
            Long panierId,
            Long laboratoireId,
            Instant dateModification,
            List<Long> essaiIds) {
    }
}
