package com.backend.modules.catalogue.service;

import com.backend.common.exception.BusinessRuleException;
import com.backend.common.exception.UnauthorizedTenantException;
import com.backend.common.tenant.TenantContext;
import com.backend.modules.catalogue.dto.AnalyseTypeDto;
import com.backend.modules.catalogue.dto.EnregistrerAnalyseRequest;
import com.backend.modules.domaine.entity.Domaine;
import com.backend.modules.domaine.repository.DomaineRepository;
import com.backend.modules.essai.entity.Essai;
import com.backend.modules.essai.repository.EssaiRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogueServiceTest {

    @Mock
    private EssaiRepository essaiRepository;
    @Mock
    private DomaineRepository domaineRepository;
    @InjectMocks
    private CatalogueService catalogueService;

    @AfterEach
    void nettoyerTenant() {
        TenantContext.clear();
    }

    @Test
    void refuseSansLaboratoireRattache() {
        TenantContext.clear();
        assertThrows(UnauthorizedTenantException.class,
                () -> catalogueService.lister(null, "TOUS", false, 0, 10));
        verify(essaiRepository, never()).rechercher(any(), anyBoolean(), anyBoolean(), any());
    }

    @Test
    void creeUneAnalyseDansLeLaboratoireCourant() {
        TenantContext.setCurrentTenant("lims_atlas");
        when(essaiRepository.existsByCodeIgnoreCase("GLY2")).thenReturn(false);
        when(domaineRepository.findByCodeIgnoreCase("biochimie"))
                .thenReturn(Optional.of(Domaine.builder().id(3L).code("BIOCHIMIE").libelle("Biochimie").build()));
        when(essaiRepository.save(any(Essai.class))).thenAnswer(invocation -> {
            Essai essai = invocation.getArgument(0);
            essai.setId(12L);
            return essai;
        });

        AnalyseTypeDto cree = catalogueService.creer(requete());

        ArgumentCaptor<Essai> capteur = ArgumentCaptor.forClass(Essai.class);
        verify(essaiRepository).save(capteur.capture());
        Essai enregistre = capteur.getValue();
        assertEquals("GLY2", enregistre.getCode());
        assertEquals("Enzymatique", enregistre.getMethode());
        assertEquals(90, enregistre.getDureeEstimee());
        assertEquals("BIOCHIMIE", enregistre.getDomaine().getCode());
        assertTrue(enregistre.getActif());
        assertEquals("Biochimie", cree.getDomaineLibelle());
        assertEquals(new BigDecimal("45.50"), cree.getPrixUnitaire());
    }

    @Test
    void refuseUnCodeDejaPresentDansLeLaboratoire() {
        TenantContext.setCurrentTenant("lims_atlas");
        when(essaiRepository.existsByCodeIgnoreCase("GLY2")).thenReturn(true);

        BusinessRuleException erreur = assertThrows(BusinessRuleException.class,
                () -> catalogueService.creer(requete()));

        assertEquals("CODE_ANALYSE_EXISTANT", erreur.getCode());
        verify(essaiRepository, never()).save(any());
    }

    private EnregistrerAnalyseRequest requete() {
        EnregistrerAnalyseRequest request = new EnregistrerAnalyseRequest();
        request.setCode(" gly2 ");
        request.setDesignation(" Glycémie ");
        request.setDescription("Dosage");
        request.setNormeReference("Enzymatique");
        request.setDureeEstimee(90);
        request.setPrixUnitaire(new BigDecimal("45.50"));
        request.setUniteMesure("g/L");
        request.setDomaineCode("biochimie");
        return request;
    }
}
