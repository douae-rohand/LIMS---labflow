package com.backend.common.security;

import com.backend.common.exception.BusinessRuleException;
import com.backend.modules.auth.security.UtilisateurPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static UtilisateurPrincipal principalCourant() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UtilisateurPrincipal principal)) {
            throw new BusinessRuleException("UTILISATEUR_NON_AUTHENTIFIE",
                    "Aucun utilisateur authentifié");
        }
        return principal;
    }
}
