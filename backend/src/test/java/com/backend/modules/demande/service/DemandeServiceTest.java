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
import com.backend.modules.demande.entity.Demande;
import com.backend.modules.demande.repository.DemandeRepository;
import com.backend.modules.utilisateur.entity.RoleUtilisateur;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertThrows;
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

    private void authentifier(Long utilisateurId) {
        UtilisateurPrincipal principal = UtilisateurPrincipal.builder()
                .id(utilisateurId)
                .email("client@test.local")
                .password("x")
                .role(RoleUtilisateur.CLIENT)
                .actif(true)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}
