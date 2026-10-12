package com.backend.common.tenant;

import com.backend.common.exception.UnauthorizedTenantException;
import com.backend.modules.auth.security.UtilisateurPrincipal;
import com.backend.modules.client.repository.ClientLaboratoireRepository;
import com.backend.modules.plateforme.entity.Laboratoire;
import com.backend.modules.utilisateur.entity.RoleUtilisateur;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantInterceptorTest {

    @Mock
    private TenantResolver tenantResolver;
    @Mock
    private ClientLaboratoireRepository clientLaboratoireRepository;

    @InjectMocks
    private TenantInterceptor interceptor;

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    void unResponsableNePeutPasForcerLeSchemaDUnAutreLaboratoire() {
        authentifier(RoleUtilisateur.RESPONSABLE, "lims_labo_a");
        when(tenantResolver.exigerActif("lims_labo_a")).thenReturn(laboratoire("A", "lims_labo_a"));
        when(tenantResolver.resoudre("lims_labo_b")).thenReturn(laboratoire("B", "lims_labo_b"));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/demandes/laboratoire/8");
        request.addHeader(TenantInterceptor.TENANT_HEADER, "lims_labo_b");

        assertThrows(UnauthorizedTenantException.class, () ->
                interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
    }

    @Test
    void unResponsableResteSurLeSchemaDeSonCompteMemeSansEnTete() {
        authentifier(RoleUtilisateur.RESPONSABLE, "lims_labo_a");
        when(tenantResolver.exigerActif("lims_labo_a")).thenReturn(laboratoire("A", "lims_labo_a"));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/demandes/laboratoire/99");

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
        assertEquals("lims_labo_a", TenantContext.getCurrentTenant());
    }

    private static Laboratoire laboratoire(String code, String schema) {
        return Laboratoire.builder()
                .code(code)
                .raisonSociale("Laboratoire " + code)
                .nomSchema(schema)
                .statut("ACTIF")
                .build();
    }

    private static void authentifier(RoleUtilisateur role, String nomSchema) {
        UtilisateurPrincipal principal = UtilisateurPrincipal.builder()
                .id(7L)
                .email("responsable@lab.test")
                .password("x")
                .role(role)
                .nomSchema(nomSchema)
                .actif(true)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}
