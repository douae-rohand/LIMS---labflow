package com.backend.common.tenant;

import com.backend.common.exception.UnauthorizedTenantException;
import com.backend.modules.auth.security.UtilisateurPrincipal;
import com.backend.modules.client.entity.ClientLaboratoire;
import com.backend.modules.client.repository.ClientLaboratoireRepository;
import com.backend.modules.plateforme.entity.Laboratoire;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantInterceptor implements HandlerInterceptor {

    public static final String TENANT_HEADER = "X-Tenant-ID";

    private final TenantResolver tenantResolver;
    private final ClientLaboratoireRepository clientLaboratoireRepository;

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 1. Requête non authentifiée ou anonyme : central uniquement, aucun repli sur X-Tenant-ID
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || !(authentication.getPrincipal() instanceof UtilisateurPrincipal principal)) {
            TenantContext.clear();
            return true;
        }

        String header = request.getHeader(TENANT_HEADER);

        // 2. Super Administrateur : central uniquement, en-tête ignoré
        if (principal.isSuperAdministrateur()) {
            TenantContext.clear();
            return true;
        }

        // 3. Client : en-tête X-Tenant-ID obligatoire, labo actif, rattaché dans client_laboratoire (ACTIF)
        if (principal.isClient()) {
            if (!StringUtils.hasText(header)) {
                throw new UnauthorizedTenantException(
                        "L'en-tête X-Tenant-ID est requis pour sélectionner un laboratoire");
            }
            Laboratoire laboratoire = tenantResolver.exigerActif(header);
            Optional<ClientLaboratoire> clientLab = clientLaboratoireRepository
                    .findByUtilisateur_IdAndLaboratoire_Id(principal.getId(), laboratoire.getId());

            if (clientLab.isEmpty() || !"ACTIF".equalsIgnoreCase(clientLab.get().getStatut())) {
                throw new UnauthorizedTenantException(
                        "Accès refusé : vous n'avez pas de compte actif dans le laboratoire " + laboratoire.getCode());
            }

            TenantContext.setCurrentTenant(laboratoire.getNomSchema());
            return true;
        }

        // 4. Personnel de laboratoire (Responsable, Admin, Technicien, etc.)
        if (principal.getNomSchema() == null) {
            throw new UnauthorizedTenantException("Employé sans laboratoire rattaché");
        }

        // Vérifier que le laboratoire de l'employé est actif
        Laboratoire laboPersonnel = tenantResolver.exigerActif(principal.getNomSchema());

        // Si X-Tenant-ID est fourni, il doit correspondre au laboratoire de l'employé
        if (StringUtils.hasText(header)) {
            Laboratoire demande = tenantResolver.resoudre(header);
            if (!laboPersonnel.getNomSchema().equals(demande.getNomSchema())) {
                throw new UnauthorizedTenantException(
                        "Accès refusé : l'en-tête X-Tenant-ID (" + header + ") ne correspond pas au laboratoire rattaché à votre compte");
            }
        }

        TenantContext.setCurrentTenant(laboPersonnel.getNomSchema());
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
