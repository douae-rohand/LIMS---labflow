package com.backend.common.tenant;

import com.backend.common.exception.UnauthorizedTenantException;
import com.backend.modules.auth.security.UtilisateurPrincipal;
import com.backend.modules.plateforme.entity.Laboratoire;
import com.backend.modules.utilisateur.entity.RoleUtilisateur;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantInterceptorTest {

    @Mock
    private TenantResolver tenantResolver;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private TenantInterceptor interceptor;

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    void clientSansLaboratoireFixeUtiliseXTenantId() throws Exception {
        authentifier(principal(RoleUtilisateur.CLIENT, null, null));
        when(request.getHeader(TenantInterceptor.TENANT_HEADER)).thenReturn("labo_a");
        when(tenantResolver.exigerActif("labo_a")).thenReturn(labo("labo_a", "lims_labo_a", "ACTIF"));

        interceptor.preHandle(request, response, new Object());

        assertEquals("lims_labo_a", TenantContext.getCurrentTenant());
    }

    @Test
    void clientSansEnTeteEstRefuse() {
        authentifier(principal(RoleUtilisateur.CLIENT, null, null));
        when(request.getHeader(TenantInterceptor.TENANT_HEADER)).thenReturn(null);

        assertThrows(UnauthorizedTenantException.class,
                () -> interceptor.preHandle(request, response, new Object()));
    }

    @Test
    void laboratoireInactifNonSelectionnable() {
        authentifier(principal(RoleUtilisateur.CLIENT, null, null));
        when(request.getHeader(TenantInterceptor.TENANT_HEADER)).thenReturn("labo_s");
        when(tenantResolver.exigerActif("labo_s"))
                .thenThrow(new UnauthorizedTenantException("Le laboratoire 'labo_s' n'est pas sélectionnable"));

        assertThrows(UnauthorizedTenantException.class,
                () -> interceptor.preHandle(request, response, new Object()));
    }

    @Test
    void employeNePeutPasChangerDeTenant() {
        authentifier(principal(RoleUtilisateur.TECHNICIEN, 10L, "lims_labo_a"));
        when(request.getHeader(TenantInterceptor.TENANT_HEADER)).thenReturn("labo_b");
        when(tenantResolver.resoudre("labo_b")).thenReturn(labo("labo_b", "lims_labo_b", "ACTIF"));

        assertThrows(UnauthorizedTenantException.class,
                () -> interceptor.preHandle(request, response, new Object()));
    }

    @Test
    void employeEstForceSurSonLaboratoire() throws Exception {
        authentifier(principal(RoleUtilisateur.TECHNICIEN, 10L, "lims_labo_a"));
        when(request.getHeader(TenantInterceptor.TENANT_HEADER)).thenReturn(null);

        interceptor.preHandle(request, response, new Object());

        assertEquals("lims_labo_a", TenantContext.getCurrentTenant());
    }

    @Test
    void tenantInconnuRefuse() {
        authentifier(principal(RoleUtilisateur.CLIENT, null, null));
        when(request.getHeader(TenantInterceptor.TENANT_HEADER)).thenReturn("inconnu");
        when(tenantResolver.exigerActif("inconnu"))
                .thenThrow(new UnauthorizedTenantException("Laboratoire introuvable : inconnu"));

        assertThrows(UnauthorizedTenantException.class,
                () -> interceptor.preHandle(request, response, new Object()));
        assertNull(TenantContext.getCurrentTenant());
    }

    private void authentifier(UtilisateurPrincipal principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private static UtilisateurPrincipal principal(RoleUtilisateur role, Long laboratoireId, String nomSchema) {
        return UtilisateurPrincipal.builder()
                .id(1L)
                .email("user@test.local")
                .password("x")
                .role(role)
                .laboratoireId(laboratoireId)
                .nomSchema(nomSchema)
                .actif(true)
                .build();
    }

    private static Laboratoire labo(String code, String schema, String statut) {
        return Laboratoire.builder().id(1L).code(code).nomSchema(schema).statut(statut).raisonSociale(code).build();
    }
}
