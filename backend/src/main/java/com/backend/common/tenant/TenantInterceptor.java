package com.backend.common.tenant;

import com.backend.common.exception.UnauthorizedTenantException;
import com.backend.modules.auth.security.UtilisateurPrincipal;
import com.backend.modules.plateforme.entity.Laboratoire;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantInterceptor implements HandlerInterceptor {

    public static final String TENANT_HEADER = "X-Tenant-ID";

    private final TenantResolver tenantResolver;

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UtilisateurPrincipal principal)) {
            return true;
        }

        String header = request.getHeader(TENANT_HEADER);

        if (principal.isSuperAdministrateur()) {
            if (StringUtils.hasText(header)) {
                TenantContext.setCurrentTenant(tenantResolver.resoudre(header).getNomSchema());
            }
            return true;
        }

        if (principal.isClient()) {
            if (!StringUtils.hasText(header)) {
                throw new UnauthorizedTenantException(
                        "L'en-tête X-Tenant-ID est requis pour choisir un laboratoire");
            }
            Laboratoire laboratoire = tenantResolver.exigerActif(header);
            TenantContext.setCurrentTenant(laboratoire.getNomSchema());
            return true;
        }

        if (principal.getNomSchema() == null) {
            throw new UnauthorizedTenantException("Employé sans laboratoire rattaché");
        }
        if (StringUtils.hasText(header)) {
            Laboratoire demande = tenantResolver.resoudre(header);
            if (!principal.getNomSchema().equals(demande.getNomSchema())) {
                throw new UnauthorizedTenantException(header, principal.getNomSchema());
            }
        }
        TenantContext.setCurrentTenant(principal.getNomSchema());
        return true;
    }

    @Override
    public void afterCompletion(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler,
            Exception ex) {
        TenantContext.clear();
    }
}
