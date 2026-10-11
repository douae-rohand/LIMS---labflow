package com.backend.modules.echantillon.service;

import com.backend.modules.conformite.dto.NonConformiteDto;
import com.backend.modules.conformite.service.ConformiteService;
import com.backend.modules.demande.repository.DemandeRepository;
import com.backend.modules.echantillon.dto.EchantillonDto;
import com.backend.modules.echantillon.entity.Echantillon;
import com.backend.modules.echantillon.entity.StatutEchantillon;
import com.backend.modules.echantillon.repository.EchantillonRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EchantillonServiceStatutTest {

    @Mock
    private EchantillonRepository echantillonRepository;
    @Mock
    private DemandeRepository demandeRepository;
    @InjectMocks
    private EchantillonService echantillonService;

    @Test
    void archiveUnEchantillonSansEffacerLaNonConformite() {
        Echantillon echantillon = echantillon(false, StatutEchantillon.NON_CONFORME);
        when(echantillonRepository.findById(1L)).thenReturn(Optional.of(echantillon));
        when(echantillonRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        EchantillonDto dto = echantillonService.changerStatut(1L, StatutEchantillon.ARCHIVE);

        assertEquals(StatutEchantillon.ARCHIVE, echantillon.getStatut());
        assertEquals(Boolean.FALSE, echantillon.getConformite());
        assertEquals(StatutEchantillon.ARCHIVE, dto.getStatut());
    }

    @Test
    void receptionnerPoseLeStatutReceptionne() {
        Echantillon echantillon = echantillon(null, StatutEchantillon.EN_ATTENTE_RECEPTION);
        when(echantillonRepository.findById(1L)).thenReturn(Optional.of(echantillon));
        when(echantillonRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        EchantillonDto dto = echantillonService.receptionner(1L, 4L);

        assertEquals(StatutEchantillon.RECEPTIONNE, dto.getStatut());
        assertEquals(Boolean.TRUE, echantillon.getConformite());
        assertNotNull(echantillon.getDateReception());
        assertEquals(4L, echantillon.getReceptionneurId());
    }

    @Test
    void declarerUneNonConformiteAligneLeStatutDeLechantillon() {
        Echantillon echantillon = echantillon(true, StatutEchantillon.RECEPTIONNE);
        echantillon.setDateReception(Instant.parse("2026-03-01T10:00:00Z"));
        when(echantillonRepository.findById(5L)).thenReturn(Optional.of(echantillon));
        when(echantillonRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ConformiteService conformiteService = new ConformiteService(echantillonRepository);

        conformiteService.declarer(NonConformiteDto.builder()
                .idEntiteSource(5L)
                .titre("Tube hémolysé")
                .build());

        assertEquals(StatutEchantillon.NON_CONFORME, echantillon.getStatut());
        assertEquals(Boolean.FALSE, echantillon.getConformite());

        conformiteService.cloturer(5L);

        assertEquals(Boolean.TRUE, echantillon.getConformite());
        assertEquals(StatutEchantillon.RECEPTIONNE, echantillon.getStatut());
    }

    private static Echantillon echantillon(Boolean conformite, StatutEchantillon statut) {
        return Echantillon.builder()
                .id(1L)
                .reference("ECH-1")
                .conformite(conformite)
                .statut(statut)
                .build();
    }
}
