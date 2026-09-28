package com.backend.modules.stock.service;

import com.backend.common.exception.BusinessRuleException;
import com.backend.common.exception.ResourceNotFoundException;
import com.backend.modules.stock.dto.ArticleStockDto;
import com.backend.modules.stock.entity.ArticleStock;
import com.backend.modules.stock.entity.MouvementStock;
import com.backend.modules.stock.entity.TypeMouvement;
import com.backend.modules.stock.repository.ArticleStockRepository;
import com.backend.modules.stock.repository.MouvementStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * Service de gestion des stocks (M09).
 * TODO: alertes de réapprovisionnement, commandes fournisseurs, QR code étiquettes.
 */
@Service @RequiredArgsConstructor
public class StockService {

    private final ArticleStockRepository articleStockRepository;
    private final MouvementStockRepository mouvementStockRepository;

    @Transactional(readOnly = true)
    public Page<ArticleStockDto> lister(Pageable pageable) {
        return articleStockRepository.findAll(pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<ArticleStockDto> alertesRupture() {
        return articleStockRepository.findEnRuptureImminente().stream().map(this::toDto).toList();
    }

    @Transactional
    public ArticleStockDto enregistrerMouvement(Long articleId, TypeMouvement type,
                                                 Double quantite, String motif, Long operateurId) {
        ArticleStock article = articleStockRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("ArticleStock", "id", articleId));

        double nouvelleQte = switch (type) {
            case ENTREE, RETOUR, AJUSTEMENT -> article.getQuantiteDisponible() + quantite;
            case SORTIE, PERTE -> {
                if (article.getQuantiteDisponible() < quantite)
                    throw new BusinessRuleException("STOCK_INSUFFISANT",
                            "Quantité disponible insuffisante pour l'article " + article.getReference());
                yield article.getQuantiteDisponible() - quantite;
            }
        };

        article.setQuantiteDisponible(nouvelleQte);
        articleStockRepository.save(article);

        mouvementStockRepository.save(MouvementStock.builder()
                .articleId(articleId).typeMouvement(type).quantite(quantite)
                .motif(motif).operateurId(operateurId).build());

        return toDto(article);
    }

    private ArticleStockDto toDto(ArticleStock a) {
        boolean rupture = a.getQuantiteMinimum() != null && a.getQuantiteDisponible() < a.getQuantiteMinimum();
        return ArticleStockDto.builder().id(a.getId()).reference(a.getReference())
                .designation(a.getDesignation()).categorie(a.getCategorie())
                .quantiteDisponible(a.getQuantiteDisponible()).quantiteMinimum(a.getQuantiteMinimum())
                .unite(a.getUnite()).prixUnitaire(a.getPrixUnitaire()).fournisseur(a.getFournisseur())
                .actif(a.isActif()).enRuptureImminente(rupture).build();
    }
}
