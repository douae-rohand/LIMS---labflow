package com.backend.modules.demande.service;

import com.backend.common.exception.BusinessRuleException;
import com.backend.common.exception.UnauthorizedTenantException;
import com.backend.common.tenant.TenantContext;
import com.backend.common.tenant.TenantExecutor;
import com.backend.modules.auth.security.UtilisateurPrincipal;
import com.backend.modules.client.entity.Client;
import com.backend.modules.client.repository.ClientLaboratoireRepository;
import com.backend.modules.client.repository.ClientRepository;
import com.backend.modules.client.service.ClientService;
import com.backend.modules.demande.dto.DemandeDto;
import com.backend.modules.demande.dto.DemandeLaboratoireDto;
import com.backend.modules.demande.entity.Demande;
import com.backend.modules.demande.entity.StatutDemande;
import com.backend.modules.demande.repository.DemandeRepository;
import com.backend.modules.demande.repository.PieceJointeRepository;
import com.backend.modules.echantillon.repository.EchantillonRepository;
import com.backend.modules.essai.repository.LigneEssaiRepository;
import com.backend.modules.utilisateur.entity.RoleUtilisateur;
import com.backend.modules.utilisateur.entity.Utilisateur;
import com.backend.modules.utilisateur.repository.UtilisateurRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemandeServiceTest {

    @Mock
    private DemandeRepository demandeRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private ClientService clientService;
    @Mock
    private ClientLaboratoireRepository clientLaboratoireRepository;
    @Mock
    private TenantExecutor tenantExecutor;
    @Mock
    private LigneEssaiRepository ligneEssaiRepository;
    @Mock
    private PieceJointeRepository pieceJointeRepository;
    @Mock
    private EchantillonRepository echantillonRepository;
    @Mock
    private UtilisateurRepository utilisateurRepository;

    @InjectMocks
    private DemandeService demandeService;

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    void refuseUnClientIdEtranger() {
        authentifier(1L);
        TenantContext.setCurrentTenant("lims_labo_a");

        DemandeDto dto = DemandeDto.builder().objet("Analyse").clientId(99L).build();

        assertThrows(BusinessRuleException.class, () -> demandeService.creer(dto));
        verify(clientService, never()).assurerFicheLocale(any(), any());
    }

    @Test
    void refuseLAccesALaDemandeDunAutreClient() {
        authentifier(1L);
        TenantContext.setCurrentTenant("lims_labo_a");
        Client autre = Client.builder().id(5L).utilisateurId(99L).raisonSociale("Autre").code("X").build();
        when(demandeRepository.findById(8L)).thenReturn(Optional.of(
                Demande.builder().id(8L).numero("DEM-1").titre("x").statut("BROUILLON").client(autre).build()));

        assertThrows(UnauthorizedTenantException.class, () -> demandeService.trouverParId(8L));
    }

    @Test
    @SuppressWarnings("unchecked")
    void creeUneDemandePourLeClientConnecteDansLeLaboratoireCourant() {
        authentifier(1L);
        TenantContext.setCurrentTenant("lims_labo_a");
        Client fiche = Client.builder().id(3L).utilisateurId(1L).raisonSociale("Client A").code("CLI-1").build();
        when(clientService.assurerFicheLocale(1L, "lims_labo_a")).thenReturn(fiche);
        when(tenantExecutor.inTenant(any(), any())).thenAnswer(invocation -> {
            Supplier<Object> supplier = invocation.getArgument(1);
            return supplier.get();
        });
        when(clientRepository.findById(3L)).thenReturn(Optional.of(fiche));
        when(demandeRepository.save(any(Demande.class))).thenAnswer(invocation -> {
            Demande d = invocation.getArgument(0);
            d.setId(11L);
            return d;
        });

        DemandeDto creee = demandeService.creer(DemandeDto.builder().objet("Demande 1").build());

        org.junit.jupiter.api.Assertions.assertEquals("Demande 1", creee.getObjet());
        org.junit.jupiter.api.Assertions.assertEquals(3L, creee.getClientId());
        org.junit.jupiter.api.Assertions.assertEquals("lims_labo_a", creee.getLaboratoireCode());
    }

    @Test
    void accepteUneDemandeSoumiseEtEnregistreLaDecision() {
        authentifier(7L, RoleUtilisateur.RESPONSABLE);
        TenantContext.setCurrentTenant("lims_labo_a");
        Demande demande = demandeSoumise(8L);
        when(demandeRepository.findById(8L)).thenReturn(Optional.of(demande));
        when(demandeRepository.save(any(Demande.class))).thenAnswer(invocation -> invocation.getArgument(0));
        preparerDetailVide(8L);
        when(tenantExecutor.inCentral(any())).thenAnswer(invocation -> {
            Supplier<Object> supplier = invocation.getArgument(0);
            return supplier.get();
        });
        when(utilisateurRepository.findAllById(any())).thenReturn(List.of(
                Utilisateur.builder().id(7L).nom("Alaoui").prenom("Sara").email("sara@lab.test")
                        .motDePasseHash("x").build()));

        DemandeLaboratoireDto resultat = demandeService.accepter(8L);

        assertEquals(StatutDemande.ACCEPTEE, resultat.getStatut());
        assertEquals(7L, resultat.getDecideurId());
        assertEquals("Sara Alaoui", resultat.getDecideurNom());
        assertNotNull(resultat.getDateDecision());
        assertTrue(demande.getDateDecision() != null);
        assertEquals(7L, demande.getDecideurId());
    }

    @Test
    void refuseUneDemandeSoumiseAvecMotif() {
        authentifier(7L, RoleUtilisateur.RESPONSABLE);
        TenantContext.setCurrentTenant("lims_labo_a");
        Demande demande = demandeSoumise(9L);
        when(demandeRepository.findById(9L)).thenReturn(Optional.of(demande));
        when(demandeRepository.save(any(Demande.class))).thenAnswer(invocation -> invocation.getArgument(0));
        preparerDetailVide(9L);
        when(tenantExecutor.inCentral(any())).thenAnswer(invocation -> {
            Supplier<Object> supplier = invocation.getArgument(0);
            return supplier.get();
        });
        when(utilisateurRepository.findAllById(any())).thenReturn(List.of());

        DemandeLaboratoireDto resultat = demandeService.refuser(9L, "  Hors périmètre du laboratoire  ");

        assertEquals(StatutDemande.REJETEE, resultat.getStatut());
        assertEquals("Hors périmètre du laboratoire", resultat.getMotif());
        assertEquals(7L, demande.getDecideurId());
        assertNotNull(demande.getDateDecision());
    }

    @Test
    void refuseUnMotifVideOuTropCourt() {
        authentifier(7L, RoleUtilisateur.RESPONSABLE);
        Demande demande = demandeSoumise(9L);
        when(demandeRepository.findById(9L)).thenReturn(Optional.of(demande));

        assertThrows(BusinessRuleException.class, () -> demandeService.refuser(9L, "   "));
        assertThrows(BusinessRuleException.class, () -> demandeService.refuser(9L, "non"));
        verify(demandeRepository, never()).save(any());
    }

    @Test
    void refuseDeTraiterUneDemandeDejaAcceptee() {
        authentifier(7L, RoleUtilisateur.RESPONSABLE);
        Demande demande = demandeSoumise(4L);
        demande.setStatut(StatutDemande.ACCEPTEE.name());
        when(demandeRepository.findById(4L)).thenReturn(Optional.of(demande));

        assertThrows(BusinessRuleException.class, () -> demandeService.accepter(4L));
        assertThrows(BusinessRuleException.class, () -> demandeService.refuser(4L, "Motif suffisamment long"));
        verify(demandeRepository, never()).save(any());
    }

    @Test
    void leChangementDeStatutNePermetPasUnRefusSansMotif() {
        authentifier(7L, RoleUtilisateur.RESPONSABLE);
        assertThrows(BusinessRuleException.class,
                () -> demandeService.changerStatut(3L, StatutDemande.REJETEE));
        verify(demandeRepository, never()).findById(org.mockito.ArgumentMatchers.anyLong());
    }

    private void preparerDetailVide(Long id) {
        when(ligneEssaiRepository.findByDemande_Id(id)).thenReturn(List.of());
        when(pieceJointeRepository.findByDemande_Id(id)).thenReturn(List.of());
        when(echantillonRepository.findByDemande_Id(org.mockito.ArgumentMatchers.eq(id), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
    }

    private Demande demandeSoumise(Long id) {
        return Demande.builder()
                .id(id)
                .numero("DEM-" + id)
                .titre("Contrôle")
                .statut(StatutDemande.SOUMISE.name())
                .dateSoumission(Instant.parse("2026-10-01T10:00:00Z"))
                .build();
    }

    private void authentifier(Long utilisateurId) {
        authentifier(utilisateurId, RoleUtilisateur.CLIENT);
    }

    private void authentifier(Long utilisateurId, RoleUtilisateur role) {
        UtilisateurPrincipal principal = UtilisateurPrincipal.builder()
                .id(utilisateurId)
                .email("client@test.local")
                .password("x")
                .role(role)
                .nomSchema(role == RoleUtilisateur.CLIENT ? null : "lims_labo_a")
                .actif(true)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}
