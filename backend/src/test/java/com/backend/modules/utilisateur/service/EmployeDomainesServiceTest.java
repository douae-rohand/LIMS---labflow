package com.backend.modules.utilisateur.service;

import com.backend.common.exception.BusinessRuleException;
import com.backend.modules.utilisateur.dto.EmployeDetailDto;
import com.backend.modules.utilisateur.dto.ModifierDomainesEmployeRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeDomainesServiceTest {

    @Mock
    private UtilisateurService utilisateurService;
    @Mock
    private TechnicienDomaineService technicienDomaineService;
    @InjectMocks
    private EmployeDomainesService service;

    @Test
    void refuseLesDomainesPourUnRoleAutreQueTechnicien() {
        when(utilisateurService.trouverEmployeParId(9L))
                .thenReturn(EmployeDetailDto.builder().id(9L).role("RESPONSABLE").build());
        ModifierDomainesEmployeRequest request = new ModifierDomainesEmployeRequest();
        request.setDomaineIds(List.of());

        assertThrows(BusinessRuleException.class, () -> service.remplacerDomaines(9L, request));

        verify(technicienDomaineService, never()).remplacerDomaines(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyList(),
                org.mockito.ArgumentMatchers.any());
    }
}
