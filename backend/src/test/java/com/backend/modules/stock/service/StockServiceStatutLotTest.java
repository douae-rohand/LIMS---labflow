package com.backend.modules.stock.service;

import com.backend.modules.produit.entity.Lot;
import com.backend.modules.produit.entity.Produit;
import com.backend.modules.produit.entity.StatutLot;
import com.backend.modules.produit.repository.LotRepository;
import com.backend.modules.produit.repository.ProduitRepository;
import com.backend.modules.stock.entity.TypeMouvement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockServiceStatutLotTest {

    @Mock
    private ProduitRepository produitRepository;
    @Mock
    private LotRepository lotRepository;
    @InjectMocks
    private StockService stockService;

    @Test
    void uneSortieAZeroEpuiseLeLotSansEcrireLeMotif() {
        Produit produit = produit();
        Lot lot = lot(new BigDecimal("5.000"), StatutLot.DISPONIBLE.name());
        when(produitRepository.findById(1L)).thenReturn(Optional.of(produit));
        when(lotRepository.findFirstByProduit_IdOrderByPeremptionAsc(1L)).thenReturn(Optional.of(lot));
        when(lotRepository.findByProduit_Id(1L)).thenReturn(List.of(lot));

        stockService.enregistrerMouvement(1L, TypeMouvement.SORTIE, 5.0,
                "casse du flacon au service qualité", 3L);

        assertEquals(0, lot.getQuantite().compareTo(BigDecimal.ZERO));
        assertEquals(StatutLot.EPUISE.name(), lot.getStatut());
    }

    @Test
    void uneEntreeSurUnLotEpuiseLeRendDisponible() {
        Produit produit = produit();
        Lot lot = lot(BigDecimal.ZERO, StatutLot.EPUISE.name());
        when(produitRepository.findById(1L)).thenReturn(Optional.of(produit));
        when(lotRepository.findFirstByProduit_IdOrderByPeremptionAsc(1L)).thenReturn(Optional.of(lot));
        when(lotRepository.findByProduit_Id(1L)).thenReturn(List.of(lot));

        stockService.enregistrerMouvement(1L, TypeMouvement.ENTREE, 2.0, "réception fournisseur", 3L);

        assertEquals(StatutLot.DISPONIBLE.name(), lot.getStatut());
        assertEquals(0, lot.getQuantite().compareTo(new BigDecimal("2.0")));
    }

    private static Produit produit() {
        return Produit.builder().id(1L).reference("GLU").nom("Glucose").unite("mL").build();
    }

    private static Lot lot(BigDecimal quantite, String statut) {
        return Lot.builder()
                .id(2L)
                .numero("LOT-1")
                .quantite(quantite)
                .peremption(LocalDate.of(2027, 1, 1))
                .statut(statut)
                .build();
    }
}
