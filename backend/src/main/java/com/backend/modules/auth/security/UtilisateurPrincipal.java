package com.backend.modules.auth.security;

import com.backend.modules.utilisateur.entity.RoleUtilisateur;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Getter
@Builder
public class UtilisateurPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final String nomComplet;
    private final RoleUtilisateur role;
    private final Long laboratoireId;
    private final String nomSchema;
    /** Statut du laboratoire rattaché, relu à chaque requête authentifiée. */
    private final String statutLaboratoire;
    private final boolean actif;
    private final boolean mustChangePassword;
    private final boolean doubleAuthentification;
    /** Codes de permission effectifs, sans le préfixe PERM_. Vide pour le Super Admin et le Client. */
    private final Set<String> permissions;

    public boolean isClient() {
        return role == RoleUtilisateur.CLIENT;
    }

    public boolean isSuperAdministrateur() {
        return role == RoleUtilisateur.SUPER_ADMINISTRATEUR;
    }

    public boolean isEmployeLaboratoire() {
        return !isClient() && !isSuperAdministrateur();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> autorites = new ArrayList<>();
        autorites.add(new SimpleGrantedAuthority("ROLE_" + role.name()));
        if (permissions != null) {
            for (String code : permissions) {
                autorites.add(new SimpleGrantedAuthority("PERM_" + code));
            }
        }
        return List.copyOf(autorites);
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return actif;
    }
}
