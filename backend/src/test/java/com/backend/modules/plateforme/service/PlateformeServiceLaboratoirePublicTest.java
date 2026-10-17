package com.backend.modules.plateforme.service;

import com.backend.common.exception.ResourceNotFoundException;
import com.backend.common.tenant.TenantExecutor;
import com.backend.common.tenant.TenantProvisioner;
import com.backend.modules.domaine.entity.Domaine;
import com.backend.modules.essai.entity.Essai;
import com.backend.modules.essai.entity.ExigenceEchantillon;
import com.backend.modules.essai.repository.EssaiRepository;
import com.backend.modules.essai.repository.ExigenceEchantillonRepository;
import com.backend.modules.plateforme.dto.AnalysePubliqueDto;
import com.backend.modules.plateforme.dto.ExigencePubliqueDto;
import com.backend.modules.plateforme.dto.LaboratoirePublicDto;
import com.backend.modules.plateforme.entity.Laboratoire;
import com.backend.modules.plateforme.repository.LaboratoireRepository;
import com.backend.modules.utilisateur.repository.RoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlateformeServiceLaboratoirePublicTest {

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
    private ExigenceEchantillonRepository exigenceEchantillonRepository;
    @Mock
    private com.backend.modules.utilisateur.repository.UtilisateurRepository utilisateurRepository;

    @InjectMocks
    private PlateformeService plateformeService;

    @Test
    void trouverPublicParIdRetourneLesCoordonneesEtLeContact() {
        when(laboratoireRepository.findById(1L)).thenReturn(Optional.of(laboratoireActif()));

        LaboratoirePublicDto dto = plateformeService.trouverPublicParId(1L);

        assertEquals(1L, dto.getId());
        assertEquals("atlas", dto.getCode());
        assertEquals("+212522000001", dto.getTelephone());
        assertEquals("contact@atlas.labflow.ma", dto.getEmail());
        assertEquals(33.5892, dto.getLatitude());
    }

    @Test
    void trouverPublicParIdIgnoreUnLaboratoireInactif() {
        when(laboratoireRepository.findById(2L)).thenReturn(Optional.of(
                Laboratoire.builder().id(2L).code("inactif").statut("DESACTIVE").build()
        ));

        assertThrows(ResourceNotFoundException.class, () -> plateformeService.trouverPublicParId(2L));
    }

    @Test
    void listerAnalysesPubliquesLitLeCatalogueTenant() {
        when(tenantExecutor.inCentral(any())).thenAnswer(invocation -> {
            Supplier<?> action = invocation.getArgument(0);
            return action.get();
        });
        when(tenantExecutor.inTenant(eq("lims_atlas"), any())).thenAnswer(invocation -> {
            Supplier<?> action = invocation.getArgument(1);
            return action.get();
        });
        when(laboratoireRepository.findById(1L)).thenReturn(Optional.of(laboratoireActif()));
        when(essaiRepository.findActifsAvecDomaine()).thenReturn(List.of(
                Essai.builder()
                        .id(10L)
                        .code("GLY")
                        .designation("Glycémie à jeun")
                        .description("Dosage du glucose sanguin")
                        .methode("Enzymatique")
                        .tarif(new BigDecimal("45.00"))
                        .dureeEstimee(60)
                        .unite("g/L")
                        .actif(true)
                        .domaine(Domaine.builder().code("BIOCHIMIE").libelle("Biochimie").build())
                        .build(),
                Essai.builder()
                        .id(11L)
                        .code("NFS")
                        .designation("Numération formule sanguine")
                        .tarif(new BigDecimal("80.00"))
                        .dureeEstimee(120)
                        .actif(true)
                        .domaine(Domaine.builder().code("HEMATOLOGIE").libelle("Hématologie").build())
                        .build()
        ));
        when(exigenceEchantillonRepository.findByEssai_IdIn(any())).thenReturn(List.of(
                ExigenceEchantillon.builder()
                        .id(100L)
                        .essai(Essai.builder().id(10L).build())
                        .nature("Sang total")
                        .etatConservation("Réfrigéré 2-8°C")
                        .quantiteMinimale(new BigDecimal("5.00"))
                        .unite("mL")
                        .build()
        ));

        List<AnalysePubliqueDto> analyses = plateformeService.listerAnalysesPubliques(1L);

        assertEquals(2, analyses.size());

        AnalysePubliqueDto glycemie = analyses.getFirst();
        assertEquals("GLY", glycemie.getCode());
        assertEquals("Biochimie", glycemie.getDomaineLibelle());
        assertEquals(new BigDecimal("45.00"), glycemie.getTarif());
        assertEquals(60, glycemie.getDureeEstimee());
        assertEquals(1, glycemie.getExigences().size());
        ExigencePubliqueDto exigence = glycemie.getExigences().getFirst();
        assertEquals("Sang total", exigence.getNature());
        assertEquals("Réfrigéré 2-8°C", exigence.getEtatConservation());
        assertEquals(new BigDecimal("5.00"), exigence.getQuantiteMinimale());
        assertEquals("mL", exigence.getUnite());

        AnalysePubliqueDto nfs = analyses.get(1);
        assertEquals("NFS", nfs.getCode());
        assertTrue(nfs.getExigences().isEmpty());
    }

    private static Laboratoire laboratoireActif() {
        return Laboratoire.builder()
                .id(1L)
                .code("atlas")
                .raisonSociale("Laboratoire Atlas")
                .ville("Casablanca")
                .adresse("12 Boulevard Zerktouni")
                .telephone("+212522000001")
                .email("contact@atlas.labflow.ma")
                .ice("001545678000012")
                .nomSchema("lims_atlas")
                .statut("ACTIF")
                .latitude(33.5892)
                .longitude(-7.6186)
                .build();
    }
}
