package com.backend.modules.auth.security;

import com.backend.modules.utilisateur.entity.RoleUtilisateur;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

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
    private final boolean actif;
    private final boolean mustChangePassword;
    private final boolean doubleAuthentification;

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
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
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
