package com.backend.modules.utilisateur.service;

import com.backend.common.audit.JournalAuditService;
import com.backend.common.tenant.TenantExecutor;
import com.backend.modules.auth.security.UtilisateurPrincipal;
import com.backend.modules.catalogue.dto.DomaineDto;
import com.backend.modules.domaine.entity.Domaine;
import com.backend.modules.domaine.repository.DomaineRepository;
import com.backend.modules.utilisateur.entity.TechnicienDomaine;
import com.backend.modules.utilisateur.repository.TechnicienDomaineRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TechnicienDomaineServiceTest {

    @Mock
    private TechnicienDomaineRepository technicienDomaineRepository;
    @Mock
    private DomaineRepository domaineRepository;
    @Mock
    private TenantExecutor tenantExecutor;
    @Mock
    private JournalAuditService journalAuditService;
    @InjectMocks
    private TechnicienDomaineService service;
    private UtilisateurPrincipal principal;

    @BeforeEach
    void authentifierAdministrateur() {
        principal = UtilisateurPrincipal.builder()
                .id(1L).email("admin@labflow.test").nomComplet("Admin")
                .role(com.backend.modules.utilisateur.entity.RoleUtilisateur.ADMINISTRATEUR)
                .laboratoireId(2L).nomSchema("lims_atlas").actif(true).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(1)).get())
                .when(tenantExecutor).inTenant(eq("lims_atlas"), any());
    }

    @AfterEach
    void deconnecter() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void neReecritPasEtNAuditePasUneListeIdentique() {
        Domaine domaine = Domaine.builder().id(3L).code("BIOCHIMIE").libelle("Biochimie").build();
        when(domaineRepository.findAllById(List.of(3L))).thenReturn(List.of(domaine));
        when(technicienDomaineRepository.findByTechnicienId(9L))
                .thenReturn(List.of(TechnicienDomaine.builder().technicienId(9L).domaineId(3L).build()));

        List<DomaineDto> resultat = service.remplacerDomaines(9L, List.of(3L), principal);

        assertEquals("BIOCHIMIE", resultat.get(0).getCode());
        verify(technicienDomaineRepository, never()).deleteByTechnicienId(9L);
        verify(technicienDomaineRepository, never()).saveAll(any());
        verify(journalAuditService, never()).enregistrer(
                any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void rejetteUnDomaineAbsentAvantTouteSuppression() {
        when(domaineRepository.findAllById(List.of(99L))).thenReturn(List.of());

        assertThrows(com.backend.common.exception.BusinessRuleException.class,
                () -> service.remplacerDomaines(9L, List.of(99L), principal));

        verify(technicienDomaineRepository, never()).deleteByTechnicienId(9L);
    }

    @Test
    void remplaceLesDomainesEtAuditeUniquementLesIdentifiants() {
        Domaine biochimie = Domaine.builder().id(3L).code("BIOCHIMIE").libelle("Biochimie").build();
        Domaine hematologie = Domaine.builder().id(7L).code("HEMATOLOGIE").libelle("Hématologie").build();
        when(domaineRepository.findAllById(List.of(3L, 7L))).thenReturn(List.of(biochimie, hematologie));
        when(technicienDomaineRepository.findByTechnicienId(9L))
                .thenReturn(List.of(TechnicienDomaine.builder().technicienId(9L).domaineId(3L).build()));

        List<DomaineDto> resultat = service.remplacerDomaines(9L, List.of(7L, 3L), principal);

        assertEquals(List.of(3L, 7L), resultat.stream().map(DomaineDto::getId).toList());
        verify(technicienDomaineRepository).deleteByTechnicienId(9L);
        verify(technicienDomaineRepository).saveAll(any());
        verify(journalAuditService).enregistrer("MODIFICATION_DOMAINES_EMPLOYE", "Utilisateur", 9L,
                "admin@labflow.test", "[3]", "[3,7]", null, 2L);
    }

}
