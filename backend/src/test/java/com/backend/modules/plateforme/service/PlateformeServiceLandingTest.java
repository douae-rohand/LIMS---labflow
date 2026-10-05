package com.backend.modules.plateforme.service;

import com.backend.common.tenant.TenantProvisioner;
import com.backend.modules.demande.entity.StatutDemande;
import com.backend.modules.plateforme.dto.LandingPublicDto;
import com.backend.modules.plateforme.entity.Laboratoire;
import com.backend.modules.plateforme.repository.DemandeIntegrationRepository;
import com.backend.modules.plateforme.repository.LaboratoireRepository;
import com.backend.modules.utilisateur.entity.Role;
import com.backend.modules.utilisateur.repository.RoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlateformeServiceLandingTest {

    @Mock
    private LaboratoireRepository laboratoireRepository;
    @Mock
    private DemandeIntegrationRepository demandeIntegrationRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private TenantProvisioner tenantProvisioner;

    @InjectMocks
    private PlateformeService plateformeService;

    @Test
    void obtenirLandingPublicLitLesRolesEtLaboratoiresActifs() {
        when(roleRepository.findAllByOrderByIdAsc()).thenReturn(List.of(
                Role.builder().id(1L).code("CLIENT").libelle("Client").build(),
                Role.builder().id(2L).code("TECHNICIEN").libelle("Technicien").build()
        ));
        when(laboratoireRepository.findByStatutOrderByRaisonSocialeAsc("ACTIF")).thenReturn(List.of(
                Laboratoire.builder()
                        .id(1L)
                        .code("lab_a")
                        .raisonSociale("Laboratoire Atlas")
                        .ville("Casablanca")
                        .statut("ACTIF")
                        .latitude(33.5892)
                        .longitude(-7.6186)
                        .build()
        ));

        LandingPublicDto dto = plateformeService.obtenirLandingPublic();

        assertEquals(2, dto.getStatistiques().getNombreRoles());
        assertEquals(1, dto.getStatistiques().getNombreLaboratoiresActifs());
        assertEquals(StatutDemande.values().length, dto.getStatistiques().getNombreStatutsDemande());
        assertEquals("CLIENT", dto.getRoles().getFirst().getCode());
        assertEquals("Client", dto.getRoles().getFirst().getLibelle());
        assertEquals("Laboratoire Atlas", dto.getLaboratoires().getFirst().getRaisonSociale());
        assertEquals("Casablanca", dto.getLaboratoires().getFirst().getVille());
        assertEquals(33.5892, dto.getLaboratoires().getFirst().getLatitude());
        assertEquals(-7.6186, dto.getLaboratoires().getFirst().getLongitude());
        assertTrue(dto.getStatutsDemande().contains("SOUMISE"));
        assertTrue(dto.getStatutsDemande().contains("ACCEPTEE"));
    }

    @Test
    void obtenirLandingPublicSansDonneesRetourneDesListesVides() {
        when(roleRepository.findAllByOrderByIdAsc()).thenReturn(List.of());
        when(laboratoireRepository.findByStatutOrderByRaisonSocialeAsc("ACTIF")).thenReturn(List.of());

        LandingPublicDto dto = plateformeService.obtenirLandingPublic();

        assertEquals(0, dto.getStatistiques().getNombreRoles());
        assertEquals(0, dto.getStatistiques().getNombreLaboratoiresActifs());
        assertTrue(dto.getRoles().isEmpty());
        assertTrue(dto.getLaboratoires().isEmpty());
        assertEquals(StatutDemande.values().length, dto.getStatutsDemande().size());
    }
}
