package com.backend.modules.planification.service;

import com.backend.modules.demande.entity.Demande;
import com.backend.modules.demande.repository.DemandeRepository;
import com.backend.modules.essai.entity.LigneEssai;
import com.backend.modules.essai.entity.StatutEssai;
import com.backend.modules.essai.repository.EssaiRepository;
import com.backend.modules.essai.repository.LigneEssaiRepository;
import com.backend.modules.planification.dto.PlanificationDto;
import com.backend.modules.planification.entity.StatutPlanification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CorrespondanceStatutLigneTest {

    @Mock
    private LigneEssaiRepository ligneEssaiRepository;
    @Mock
    private DemandeRepository demandeRepository;
    @Mock
    private EssaiRepository essaiRepository;
    @InjectMocks
    private PlanificationService planificationService;

    @Test
    void traduitLeVocabulairePlanificationVersLaLigne() {
        assertEquals(StatutEssai.PLANIFIEE, CorrespondanceStatutLigne.versLigne(StatutPlanification.PLANIFIEE));
        assertEquals(StatutEssai.EN_COURS, CorrespondanceStatutLigne.versLigne(StatutPlanification.EN_COURS));
        assertEquals(StatutEssai.TERMINE, CorrespondanceStatutLigne.versLigne(StatutPlanification.TERMINEE));
        assertEquals(StatutEssai.ANNULE, CorrespondanceStatutLigne.versLigne(StatutPlanification.ANNULEE));
        assertEquals(StatutEssai.REPORTEE, CorrespondanceStatutLigne.versLigne(StatutPlanification.REPORTEE));
        assertEquals(StatutEssai.PLANIFIEE, CorrespondanceStatutLigne.versLigne(null));
    }

    @Test
    void relitLeStatutCanoniqueDansLeVocabulairePlanification() {
        assertEquals(StatutPlanification.TERMINEE, CorrespondanceStatutLigne.versPlanification(StatutEssai.TERMINE));
        assertEquals(StatutPlanification.ANNULEE, CorrespondanceStatutLigne.versPlanification(StatutEssai.ANNULE));
        assertNull(CorrespondanceStatutLigne.versPlanification(StatutEssai.EN_ATTENTE));
        assertNull(CorrespondanceStatutLigne.versPlanification(StatutEssai.A_REFAIRE));
    }

    @Test
    void changerStatutPersisteLeNomCanonique() {
        LigneEssai ligne = LigneEssai.builder()
                .id(3L)
                .code("PLN-1")
                .statut(StatutEssai.PLANIFIEE)
                .montant(BigDecimal.ZERO)
                .demande(Demande.builder().id(9L).numero("DEM-1").titre("Bilan").statut("SOUMISE").build())
                .build();
        when(ligneEssaiRepository.findById(3L)).thenReturn(Optional.of(ligne));
        when(ligneEssaiRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PlanificationDto dto = planificationService.changerStatut(3L, StatutPlanification.TERMINEE);

        assertEquals(StatutEssai.TERMINE, ligne.getStatut());
        assertEquals(StatutPlanification.TERMINEE, dto.getStatut());
    }
}
