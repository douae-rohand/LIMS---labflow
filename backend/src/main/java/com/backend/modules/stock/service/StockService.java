package com.backend.modules.stock.service;

import com.backend.common.exception.BusinessRuleException;
import com.backend.common.exception.ResourceNotFoundException;
import com.backend.modules.produit.entity.Lot;
import com.backend.modules.produit.entity.Produit;
import com.backend.modules.produit.repository.LotRepository;
import com.backend.modules.produit.repository.ProduitRepository;
import com.backend.modules.stock.dto.ArticleStockDto;
import com.backend.modules.stock.entity.TypeMouvement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockService {

    private final ProduitRepository produitRepository;
    private final LotRepository lotRepository;

    @Transactional(readOnly = true)
    public Page<ArticleStockDto> lister(Pageable pageable) {
        return produitRepository.findAll(pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<ArticleStockDto> alertesRupture() {
        return produitRepository.findAll().stream()
                .filter(this::enRupture)
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public ArticleStockDto enregistrerMouvement(Long articleId, TypeMouvement type,
                                                 Double quantite, String motif, Long operateurId) {
        Produit produit = produitRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Produit", "id", articleId));
        Lot lot = lotRepository.findFirstByProduit_IdOrderByPeremptionAsc(produit.getId())
                .orElseThrow(() -> new BusinessRuleException("LOT_INTROUVABLE",
                        "Aucun lot pour le produit " + produit.getReference()));

        BigDecimal delta = BigDecimal.valueOf(quantite);
        BigDecimal actuelle = lot.getQuantite() == null ? BigDecimal.ZERO : lot.getQuantite();
        BigDecimal nouvelle = switch (type) {
            case ENTREE, RETOUR, AJUSTEMENT -> actuelle.add(delta);
            case SORTIE, PERTE -> {
                if (actuelle.compareTo(delta) < 0) {
                    throw new BusinessRuleException("STOCK_INSUFFISANT",
                            "Quantité disponible insuffisante pour l'article " + produit.getReference());
                }
                yield actuelle.subtract(delta);
            }
        };
        lot.setQuantite(nouvelle);
        if (motif != null && !motif.isBlank()) {
            lot.setStatut(motif);
        }
        lotRepository.save(lot);
        return toDto(produit);
    }

    private boolean enRupture(Produit produit) {
        BigDecimal seuil = produit.getSeuilMinimal() == null ? BigDecimal.ZERO : produit.getSeuilMinimal();
        return quantiteTotale(produit).compareTo(seuil) < 0;
    }

    private BigDecimal quantiteTotale(Produit produit) {
        return lotRepository.findByProduit_Id(produit.getId()).stream()
                .map(lot -> lot.getQuantite() == null ? BigDecimal.ZERO : lot.getQuantite())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private ArticleStockDto toDto(Produit produit) {
        BigDecimal totale = quantiteTotale(produit);
        BigDecimal seuil = produit.getSeuilMinimal() == null ? BigDecimal.ZERO : produit.getSeuilMinimal();
        return ArticleStockDto.builder()
                .id(produit.getId())
                .reference(produit.getReference())
                .designation(produit.getNom())
                .quantiteDisponible(totale.doubleValue())
                .quantiteMinimum(seuil.doubleValue())
                .unite(produit.getUnite())
                .actif(true)
                .enRuptureImminente(totale.compareTo(seuil) < 0)
                .build();
    }
}
