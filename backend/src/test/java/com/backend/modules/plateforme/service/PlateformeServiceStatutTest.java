package com.backend.modules.plateforme.service;

import com.backend.common.tenant.TenantExecutor;
import com.backend.common.tenant.TenantProvisioner;
import com.backend.modules.essai.repository.EssaiRepository;
import com.backend.modules.plateforme.entity.Laboratoire;
import com.backend.modules.plateforme.repository.LaboratoireRepository;
import com.backend.modules.utilisateur.repository.RoleRepository;
import com.backend.modules.utilisateur.repository.UtilisateurRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlateformeServiceStatutTest {

    @Mock
    private LaboratoireRepository laboratoireRepository;
    @Mock
    private com.backend.modules.plateforme.repository.DemandeIntegrationRepository demandeIntegrationRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private TenantProvisioner tenantProvisioner;
    @Mock
    private TenantExecutor tenantExecutor;
    @Mock
    private EssaiRepository essaiRepository;
    @Mock
    private UtilisateurRepository utilisateurRepository;

    @InjectMocks
    private PlateformeService plateformeService;

    @Test
    void desactiverConserveLeSchemaEtLesDonnees() {
        Laboratoire laboratoire = Laboratoire.builder()
                .id(4L)
                .code("atlas")
                .raisonSociale("Laboratoire Atlas")
                .nomSchema("lims_atlas")
                .statut("ACTIF")
                .build();
        when(laboratoireRepository.findById(4L)).thenReturn(Optional.of(laboratoire));
        when(laboratoireRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var dto = plateformeService.desactiverLaboratoire(4L);

        assertEquals("INACTIF", laboratoire.getStatut());
        assertEquals("lims_atlas", laboratoire.getNomSchema());
        assertEquals(false, dto.isActif());
        verify(tenantProvisioner, never()).provisionner(any());
    }

    @Test
    void activerRetablitLAccesSansRecreerLeTenant() {
        Laboratoire laboratoire = Laboratoire.builder()
                .id(4L)
                .code("atlas")
                .raisonSociale("Laboratoire Atlas")
                .nomSchema("lims_atlas")
                .statut("INACTIF")
                .build();
        when(laboratoireRepository.findById(4L)).thenReturn(Optional.of(laboratoire));
        when(laboratoireRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var dto = plateformeService.activerLaboratoire(4L);

        assertEquals("ACTIF", laboratoire.getStatut());
        assertEquals("lims_atlas", dto.getSchemaName());
        assertEquals(true, dto.isActif());
        verify(tenantProvisioner, never()).provisionner(any());
    }

    @Test
    void statistiquesDistinguentLaboratoiresEtDemandesOuvertes() {
        when(laboratoireRepository.count()).thenReturn(8L);
        when(laboratoireRepository.countByStatut("ACTIF")).thenReturn(6L);
        when(laboratoireRepository.countByStatut("INACTIF")).thenReturn(2L);
        when(demandeIntegrationRepository.countByStatut("EN_ATTENTE")).thenReturn(3L);
        when(demandeIntegrationRepository.countByStatut("EN_COURS_VALIDATION")).thenReturn(1L);
        when(demandeIntegrationRepository.count()).thenReturn(12L);

        var stats = plateformeService.statistiques();

        assertEquals(8L, stats.getTotalLaboratoires());
        assertEquals(6L, stats.getLaboratoiresActifs());
        assertEquals(2L, stats.getLaboratoiresDesactives());
        assertEquals(4L, stats.getDemandesEnAttente());
        assertEquals(12L, stats.getTotalDemandes());
    }
}
