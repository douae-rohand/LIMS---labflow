package com.backend.modules.echantillon.service;

import com.backend.modules.auth.security.UtilisateurPrincipal;
import com.backend.modules.conformite.dto.NonConformiteDto;
import com.backend.modules.conformite.service.ConformiteService;
import com.backend.modules.demande.entity.Demande;
import com.backend.modules.demande.repository.DemandeRepository;
import com.backend.modules.echantillon.dto.EchantillonDto;
import com.backend.modules.echantillon.entity.Echantillon;
import com.backend.modules.echantillon.repository.EchantillonRepository;
import com.backend.modules.utilisateur.entity.RoleUtilisateur;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

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

    @AfterEach
    void nettoyerContexte() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void declarerUneNonConformiteLaisseLaConformiteAFaux() {
        Echantillon echantillon = echantillon(true);
        when(echantillonRepository.findById(1L)).thenReturn(Optional.of(echantillon));
        when(echantillonRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ConformiteService conformiteService = new ConformiteService(echantillonRepository);

        conformiteService.declarer(NonConformiteDto.builder()
                .idEntiteSource(1L)
                .titre("Tube hémolysé")
                .build());

        assertEquals(Boolean.FALSE, echantillon.getConformite());
        assertEquals("Tube hémolysé", echantillon.getMotif());
    }

    @Test
    void enregistrerPoseLaReceptionSurLePrincipal() {
        authentifier(4L);
        when(demandeRepository.findById(9L)).thenReturn(Optional.of(Demande.builder().id(9L).build()));
        when(echantillonRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        EchantillonDto dto = echantillonService.enregistrer(EchantillonDto.builder()
                .demandeId(9L)
                .nature("Sang")
                .codeBarre("ECH-1")
                .receptionneurId(99L)
                .build());

        assertEquals(4L, dto.getReceptionneurId());
        assertNotNull(dto.getDateReception());
        assertEquals(Boolean.TRUE, dto.getConformite());
        assertEquals("Sang", dto.getNature());
    }

    @Test
    void cloturerNeToucheQueLaConformite() {
        Echantillon echantillon = echantillon(false);
        echantillon.setMotif("Tube hémolysé");
        echantillon.setDateReception(Instant.parse("2026-03-01T10:00:00Z"));
        when(echantillonRepository.findById(5L)).thenReturn(Optional.of(echantillon));
        when(echantillonRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ConformiteService conformiteService = new ConformiteService(echantillonRepository);

        conformiteService.cloturer(5L);

        assertEquals(Boolean.TRUE, echantillon.getConformite());
        assertEquals("Tube hémolysé", echantillon.getMotif());
        assertEquals(Instant.parse("2026-03-01T10:00:00Z"), echantillon.getDateReception());
    }

    private static Echantillon echantillon(Boolean conformite) {
        return Echantillon.builder()
                .id(1L)
                .reference("ECH-1")
                .nature("Sang")
                .dateReception(Instant.parse("2026-03-01T10:00:00Z"))
                .receptionneurId(4L)
                .conformite(conformite)
                .build();
    }

    private static void authentifier(Long utilisateurId) {
        UtilisateurPrincipal principal = UtilisateurPrincipal.builder()
                .id(utilisateurId)
                .email("accueil@test.local")
                .password("x")
                .role(RoleUtilisateur.ACCUEIL)
                .nomSchema("lims_atlas")
                .actif(true)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}
