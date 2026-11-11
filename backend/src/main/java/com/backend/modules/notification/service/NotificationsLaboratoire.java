package com.backend.modules.notification.service;

import com.backend.common.tenant.TenantExecutor;
import com.backend.common.util.ApresCommit;
import com.backend.modules.utilisateur.entity.RoleUtilisateur;
import com.backend.modules.utilisateur.entity.Utilisateur;
import com.backend.modules.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Notifications in-app vers les comptes d'un rôle dans un laboratoire (Responsable, Accueil…).
 * Envoyées après le commit de l'action métier, dans le schéma central où vivent les comptes :
 * un échec est journalisé sans jamais annuler l'action.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationsLaboratoire {

    private final NotificationService notificationService;
    private final UtilisateurRepository utilisateurRepository;
    private final TenantExecutor tenantExecutor;

    public void notifierApresCommit(RoleUtilisateur role, Long laboratoireId, String type, String titre,
                                    String corps, String lien) {
        if (laboratoireId == null) {
            return;
        }
        ApresCommit.executer(() -> {
            try {
                tenantExecutor.inCentral(() -> {
                    List<Long> destinataires = utilisateurRepository
                            .findActifsParRoleEtLaboratoire(role.name(), laboratoireId)
                            .stream().map(Utilisateur::getId).toList();
                    notificationService.notifierUtilisateurs(destinataires, type, titre, corps, lien);
                    return null;
                });
            } catch (RuntimeException ex) {
                log.warn("Notification « {} » non envoyée aux {} : {}", titre, role, ex.getMessage());
            }
        });
    }
}
