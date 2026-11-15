package com.backend.common.tempsreel;

import com.backend.modules.utilisateur.entity.RoleUtilisateur;

import java.security.Principal;

/**
 * Identité d'une connexion WebSocket, établie à partir du JWT lors du CONNECT STOMP.
 * Le nom est l'identifiant de l'utilisateur : c'est la clé utilisée par
 * {@code convertAndSendToUser(id, ...)} pour atteindre sa file personnelle.
 */
public record UtilisateurConnecte(Long id, RoleUtilisateur role, Long laboratoireId) implements Principal {

    @Override
    public String getName() {
        return String.valueOf(id);
    }

    public boolean superAdministrateur() {
        return role == RoleUtilisateur.SUPER_ADMINISTRATEUR;
    }

    public boolean client() {
        return role == RoleUtilisateur.CLIENT;
    }
}
