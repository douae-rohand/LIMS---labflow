package com.backend.modules.utilisateur.service;

import com.backend.modules.auth.security.AutorisationService;
import com.backend.modules.auth.security.UtilisateurPrincipal;
import com.backend.modules.utilisateur.entity.RoleUtilisateur;
import com.backend.modules.utilisateur.entity.Utilisateur;
import com.backend.modules.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UtilisateurDetailsService implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepository;
    private final AutorisationService autorisationService;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Utilisateur utilisateur = utilisateurRepository.findByEmailWithRoleAndLaboratoire(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Utilisateur introuvable avec l'email : " + email));
        return versPrincipal(utilisateur, autorisationService.effectives(utilisateur.getId(), utilisateur.getRole().getCode()));
    }

    public static UtilisateurPrincipal versPrincipal(Utilisateur utilisateur, java.util.Set<String> permissions) {
        RoleUtilisateur role = RoleUtilisateur.valueOf(utilisateur.getRole().getCode());
        Long laboratoireId = utilisateur.getLaboratoire() == null ? null : utilisateur.getLaboratoire().getId();
        String nomSchema = utilisateur.getLaboratoire() == null ? null : utilisateur.getLaboratoire().getNomSchema();
        String statutLaboratoire = utilisateur.getLaboratoire() == null ? null : utilisateur.getLaboratoire().getStatut();
        return UtilisateurPrincipal.builder()
                .id(utilisateur.getId())
                .email(utilisateur.getEmail())
                .password(utilisateur.getMotDePasseHash())
                .nomComplet(utilisateur.getNomComplet())
                .role(role)
                .laboratoireId(laboratoireId)
                .nomSchema(nomSchema)
                .statutLaboratoire(statutLaboratoire)
                .actif(utilisateur.isActif())
                .mustChangePassword(utilisateur.isMustChangePassword())
                .doubleAuthentification(utilisateur.isDoubleAuthentification())
                .secret2faPresent(utilisateur.getSecret2fa() != null && !utilisateur.getSecret2fa().isBlank())
                .permissions(permissions == null ? java.util.Set.of() : permissions)
                .build();
    }
}
