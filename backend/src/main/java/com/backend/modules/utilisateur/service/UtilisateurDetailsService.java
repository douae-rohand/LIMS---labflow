package com.backend.modules.utilisateur.service;

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

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Utilisateur utilisateur = utilisateurRepository.findByEmailWithRoleAndLaboratoire(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Utilisateur introuvable avec l'email : " + email));
        return versPrincipal(utilisateur);
    }

    public static UtilisateurPrincipal versPrincipal(Utilisateur utilisateur) {
        RoleUtilisateur role = RoleUtilisateur.valueOf(utilisateur.getRole().getCode());
        Long laboratoireId = utilisateur.getLaboratoire() == null ? null : utilisateur.getLaboratoire().getId();
        String nomSchema = utilisateur.getLaboratoire() == null ? null : utilisateur.getLaboratoire().getNomSchema();
        return UtilisateurPrincipal.builder()
                .id(utilisateur.getId())
                .email(utilisateur.getEmail())
                .password(utilisateur.getMotDePasseHash())
                .nomComplet(utilisateur.getNomComplet())
                .role(role)
                .laboratoireId(laboratoireId)
                .nomSchema(nomSchema)
                .actif(utilisateur.isActif())
                .build();
    }
}
