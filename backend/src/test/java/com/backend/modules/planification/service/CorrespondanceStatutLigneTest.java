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
        assertEquals(StatutEssai.ATTENTE_ECHANTILLON, CorrespondanceStatutLigne.versLigne(StatutPlanification.PLANIFIEE));
        assertEquals(StatutEssai.EN_COURS, CorrespondanceStatutLigne.versLigne(StatutPlanification.EN_COURS));
        assertEquals(StatutEssai.TERMINE, CorrespondanceStatutLigne.versLigne(StatutPlanification.TERMINEE));
        assertEquals(StatutEssai.NON_REALISABLE, CorrespondanceStatutLigne.versLigne(StatutPlanification.ANNULEE));
        assertEquals(StatutEssai.ATTENTE_ECHANTILLON, CorrespondanceStatutLigne.versLigne(StatutPlanification.REPORTEE));
        assertEquals(StatutEssai.ATTENTE_ECHANTILLON, CorrespondanceStatutLigne.versLigne(null));
    }

    @Test
    void relitLeStatutCanoniqueDansLeVocabulairePlanification() {
        assertEquals(StatutPlanification.PLANIFIEE, CorrespondanceStatutLigne.versPlanification(StatutEssai.ATTENTE_ECHANTILLON));
        assertEquals(StatutPlanification.TERMINEE, CorrespondanceStatutLigne.versPlanification(StatutEssai.TERMINE));
        assertEquals(StatutPlanification.ANNULEE, CorrespondanceStatutLigne.versPlanification(StatutEssai.NON_REALISABLE));
    }

    @Test
    void changerStatutPersisteLeNomCanonique() {
        LigneEssai ligne = LigneEssai.builder()
                .id(3L)
                .code("PLN-1")
                .statut(StatutEssai.ATTENTE_ECHANTILLON)
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
