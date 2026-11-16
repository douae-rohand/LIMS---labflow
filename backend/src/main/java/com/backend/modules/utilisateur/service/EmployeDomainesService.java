package com.backend.modules.utilisateur.service;

import com.backend.common.exception.BusinessRuleException;
import com.backend.common.security.SecurityUtils;
import com.backend.modules.auth.security.UtilisateurPrincipal;
import com.backend.modules.utilisateur.dto.EmployeDetailDto;
import com.backend.modules.utilisateur.dto.ModifierDomainesEmployeRequest;
import com.backend.modules.utilisateur.dto.ModifierEmployeRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmployeDomainesService {

    private static final String ROLE_TECHNICIEN = "TECHNICIEN";

    private final UtilisateurService utilisateurService;
    private final TechnicienDomaineService technicienDomaineService;

    public EmployeDetailDto trouverEmployeParId(Long id) {
        return technicienDomaineService.completerDomaines(utilisateurService.trouverEmployeParId(id));
    }

    public EmployeDetailDto modifierEmploye(Long id, ModifierEmployeRequest request) {
        EmployeDetailDto employe = utilisateurService.modifierEmploye(id, request);
        if (!ROLE_TECHNICIEN.equals(employe.getRole())) {
            technicienDomaineService.supprimerDomaines(id, SecurityUtils.principalCourant());
        }
        return technicienDomaineService.completerDomaines(employe);
    }

    public EmployeDetailDto desactiverEmploye(Long id) {
        return technicienDomaineService.completerDomaines(utilisateurService.desactiverEmploye(id));
    }

    public EmployeDetailDto reactiverEmploye(Long id) {
        return technicienDomaineService.completerDomaines(utilisateurService.reactiverEmploye(id));
    }

    public EmployeDetailDto renvoyerInvitationEmploye(Long id) {
        return technicienDomaineService.completerDomaines(utilisateurService.renvoyerInvitationEmploye(id));
    }

    public EmployeDetailDto remplacerDomaines(Long id, ModifierDomainesEmployeRequest request) {
        EmployeDetailDto employe = utilisateurService.trouverEmployeParId(id);
        if (!ROLE_TECHNICIEN.equals(employe.getRole())) {
            throw new BusinessRuleException("ROLE_NON_TECHNICIEN",
                    "Les domaines de compétence ne peuvent être attribués qu'à un technicien.");
        }
        employe.setDomaines(technicienDomaineService.remplacerDomaines(
                id, request.getDomaineIds(), SecurityUtils.principalCourant()));
        return employe;
    }
}
