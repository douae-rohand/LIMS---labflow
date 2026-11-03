package com.backend.modules.auth.security;

import com.backend.modules.utilisateur.entity.RoleUtilisateur;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 2FA obligatoire pour le responsable et l'administrateur de laboratoire.
 * Le super administrateur reste facultatif. La propriété permet de couper l'obligation.
 */
@Component
public class ObligationDeuxFacteurs {

    @Value("${app.securite.2fa-obligatoire:true}")
    private boolean obligatoire;

    public boolean requise(RoleUtilisateur role) {
        if (!obligatoire || role == null) {
            return false;
        }
        return role == RoleUtilisateur.RESPONSABLE || role == RoleUtilisateur.ADMINISTRATEUR;
    }

    public boolean accesAutorise(UtilisateurPrincipal principal) {
        if (principal == null) {
            return false;
        }
        boolean secretPresent = principal.isSecret2faPresent();
        boolean enrolee = principal.isDoubleAuthentification() && secretPresent;
        if (principal.isDoubleAuthentification() && !secretPresent) {
            return false;
        }
        return !requise(principal.getRole()) || enrolee;
    }
}
