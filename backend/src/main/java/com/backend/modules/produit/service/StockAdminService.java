package com.backend.modules.produit.service;

import com.backend.common.audit.JournalAuditService;
import com.backend.common.exception.BusinessRuleException;
import com.backend.common.exception.ResourceNotFoundException;
import com.backend.common.security.SecurityUtils;
import com.backend.modules.produit.dto.*;
import com.backend.modules.produit.entity.Lot;
import com.backend.modules.produit.entity.Produit;
import com.backend.modules.produit.repository.LotRepository;
import com.backend.modules.produit.repository.ProduitRepository;
import com.backend.modules.stock.entity.TypeMouvement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockAdminService {

    private final ProduitRepository produitRepository;
    private final LotRepository lotRepository;
    private final JournalAuditService journalAuditService;

    private static final int DELAI_PEREMPTION_DEFAUT = 30;

    @Transactional(readOnly = true)
    public Page<ProduitDto> listerProduits(Pageable pageable) {
        Page<Produit> page = produitRepository.findAll(pageable);
        return page.map(this::toProduitDto);
    }

    @Transactional(readOnly = true)
    public ProduitDto getProduit(Long id) {
        Produit produit = produitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit", "id", id));
        return toProduitDto(produit);
    }

    @Transactional
    public ProduitDto creerProduit(CreerProduitRequest request) {
        if (produitRepository.existsByReference(request.getReference())) {
            throw new BusinessRuleException("PRODUIT_REFERENCE_EXISTANTE",
                    "Un produit avec cette référence existe déjà");
        }

        Produit produit = Produit.builder()
                .reference(request.getReference())
                .nom(request.getNom())
                .unite(request.getUnite())
                .seuilMinimal(request.getSeuilMinimal())
                .conditionsStockage(request.getConditionsStockage())
                .actif(true)
                .build();

        produit = produitRepository.save(produit);

        journalAuditService.enregistrer("CREATION_PRODUIT", "Produit", produit.getId(),
                SecurityUtils.principalCourant().getEmail(), null, null, null);

        return toProduitDto(produit);
    }

    @Transactional
    public ProduitDto modifierProduit(Long id, ModifierProduitRequest request) {
        Produit produit = produitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit", "id", id));

        if (request.getNom() != null) produit.setNom(request.getNom());
        if (request.getUnite() != null) produit.setUnite(request.getUnite());
        if (request.getSeuilMinimal() != null) produit.setSeuilMinimal(request.getSeuilMinimal());
        if (request.getConditionsStockage() != null) produit.setConditionsStockage(request.getConditionsStockage());
        if (request.getActif() != null) produit.setActif(request.getActif());

        produit = produitRepository.save(produit);

        journalAuditService.enregistrer("MODIFICATION_PRODUIT", "Produit", produit.getId(),
                SecurityUtils.principalCourant().getEmail(), null, null, null);

        return toProduitDto(produit);
    }

    @Transactional
    public void supprimerProduit(Long id) {
        Produit produit = produitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit", "id", id));

        if (lotRepository.existsByProduitId(id)) {
            throw new BusinessRuleException("PRODUIT_AVEC_LOTS",
                    "Ce produit possède des lots et ne peut pas être supprimé.");
        }

        produitRepository.delete(produit);

        journalAuditService.enregistrer("SUPPRESSION_PRODUIT", "Produit", id,
                SecurityUtils.principalCourant().getEmail(), null, null, null);
    }

    @Transactional(readOnly = true)
    public List<LotDto> listerLotsParProduit(Long produitId) {
        return lotRepository.findByProduitId(produitId).stream()
                .map(this::toLotDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public LotDto creerLot(CreerLotRequest request) {
        Produit produit = produitRepository.findById(request.getProduitId())
                .orElseThrow(() -> new ResourceNotFoundException("Produit", "id", request.getProduitId()));

        if (lotRepository.existsByProduitIdAndNumero(request.getProduitId(), request.getNumero())) {
            throw new BusinessRuleException("LOT_DEJA_EXISTANT",
                    "Un lot avec ce numéro existe déjà pour ce produit");
        }

        Lot lot = Lot.builder()
                .numero(request.getNumero())
                .quantite(request.getQuantite())
                .peremption(request.getPeremption())
                .statut("DISPONIBLE")
                .produit(produit)
                .build();

        lot = lotRepository.save(lot);

        journalAuditService.enregistrer("ENTREE_LOT", "Lot", lot.getId(),
                SecurityUtils.principalCourant().getEmail(), "{\"quantite\": 0}",
                "{\"quantite\": " + lot.getQuantite() + "}", null);

        return toLotDto(lot);
    }

    @Transactional
    public LotDto ajusterStock(Long lotId, AjusterStockRequest request) {
        Lot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new ResourceNotFoundException("Lot", "id", lotId));

        BigDecimal quantiteAvant = lot.getQuantite();
        BigDecimal nouvelleQuantite;

        if (request.getType() == TypeMouvement.SORTIE || request.getType() == TypeMouvement.PERTE) {
            nouvelleQuantite = quantiteAvant.subtract(request.getQuantite());
        } else if (request.getType() == TypeMouvement.ENTREE) {
            nouvelleQuantite = quantiteAvant.add(request.getQuantite());
        } else {
            nouvelleQuantite = request.getQuantite();
        }

        if (nouvelleQuantite.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("STOCK_INSUFFISANT",
                    "La quantité ajustée rendrait le stock négatif");
        }

        lot.setQuantite(nouvelleQuantite);
        lot = lotRepository.save(lot);

        journalAuditService.enregistrer("AJUSTEMENT_STOCK", "Lot", lot.getId(),
                SecurityUtils.principalCourant().getEmail(),
                "{\"quantite\": " + quantiteAvant + "}",
                "{\"quantite\": " + nouvelleQuantite + ", \"motif\": \"" + request.getMotif() + "\"}", null);

        return toLotDto(lot);
    }

    @Transactional(readOnly = true)
    public Map<String, List<?>> getAlertes() {
        LocalDate peremptionProche = LocalDate.now().plusDays(DELAI_PEREMPTION_DEFAUT);

        List<Produit> sousSeuil = produitRepository.findProduitsSousSeuil();
        List<Produit> enRupture = produitRepository.findProduitsEnRupture();
        List<Lot> perimes = lotRepository.findLotsPerimes();
        List<Lot> peremptionProcheLots = lotRepository.findLotsPeremptionProche(peremptionProche);

        return Map.of(
                "sousSeuil", sousSeuil.stream().map(this::toProduitDto).collect(Collectors.toList()),
                "rupture", enRupture.stream().map(this::toProduitDto).collect(Collectors.toList()),
                "perimes", perimes.stream().map(this::toLotDto).collect(Collectors.toList()),
                "peremptionProche", peremptionProcheLots.stream().map(this::toLotDto).collect(Collectors.toList())
        );
    }

    @Transactional(readOnly = true)
    public Page<ProduitDto> tousProduits(Pageable pageable) {
        return produitRepository.findAll(pageable).map(this::toProduitDto);
    }

    @Transactional(readOnly = true)
    public Page<Object[]> tousEmployes(Pageable pageable) {
        return produitRepository.listeEmployes(pageable);
    }

    private ProduitDto toProduitDto(Produit produit) {
        BigDecimal stockCourant = lotRepository.sumQuantiteByProduitId(produit.getId());
        if (stockCourant == null) stockCourant = BigDecimal.ZERO;

        return ProduitDto.builder()
                .id(produit.getId())
                .reference(produit.getReference())
                .nom(produit.getNom())
                .unite(produit.getUnite())
                .seuilMinimal(produit.getSeuilMinimal())
                .conditionsStockage(produit.getConditionsStockage())
                .actif(produit.getActif())
                .stockCourant(stockCourant)
                .build();
    }

    private LotDto toLotDto(Lot lot) {
        return LotDto.builder()
                .id(lot.getId())
                .numero(lot.getNumero())
                .quantite(lot.getQuantite())
                .peremption(lot.getPeremption())
                .statut(lot.getStatut())
                .produitId(lot.getProduit() != null ? lot.getProduit().getId() : null)
                .produitReference(lot.getProduit() != null ? lot.getProduit().getReference() : null)
                .build();
    }
}
