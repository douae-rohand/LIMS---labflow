package com.backend.common.tenant;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Intercepteur HTTP qui résout l'identifiant du tenant courant.
 *
 * <p>Ordre de priorité :
 * <ol>
 *   <li>Claim {@code tenantId} du JWT → positionné par {@code JwtAuthenticationFilter}
 *       dans {@link TenantContext} <em>avant</em> l'exécution de cet intercepteur.</li>
 *   <li>En-tête HTTP {@code X-Tenant-ID} (fallback pour des appels inter-services).</li>
 * </ol>
 *
 * <p>Le nettoyage du {@link TenantContext} est effectué dans
 * {@code JwtAuthenticationFilter#finally} ; l'intercepteur fait un nettoyage
 * de sécurité supplémentaire dans {@code afterCompletion}.
 */
@Slf4j
@Component
public class TenantInterceptor implements HandlerInterceptor {

    public static final String TENANT_HEADER = "X-Tenant-ID";

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler) {

        // Si le filtre JWT a déjà positionné le tenant, on ne fait rien
        if (TenantContext.getCurrentTenant() == null) {
            String tenantFromHeader = request.getHeader(TENANT_HEADER);
            if (StringUtils.hasText(tenantFromHeader)) {
                log.debug("Tenant résolu depuis l'en-tête X-Tenant-ID : {}", tenantFromHeader);
                TenantContext.setCurrentTenant(tenantFromHeader);
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler,
            Exception ex) {
        // Sécurité : s'assurer que le ThreadLocal est toujours libéré
        TenantContext.clear();
    }
}
