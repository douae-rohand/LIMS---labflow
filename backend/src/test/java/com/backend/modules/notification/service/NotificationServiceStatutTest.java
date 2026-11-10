package com.backend.modules.notification.service;

import com.backend.modules.notification.dto.NotificationDto;
import com.backend.modules.notification.entity.CanalNotification;
import com.backend.modules.notification.entity.Notification;
import com.backend.modules.notification.entity.NotificationReception;
import com.backend.modules.notification.entity.StatutEnvoi;
import com.backend.modules.notification.repository.NotificationReceptionRepository;
import com.backend.modules.notification.repository.NotificationRepository;
import com.backend.modules.utilisateur.entity.Utilisateur;
import com.backend.modules.utilisateur.repository.UtilisateurRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceStatutTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationReceptionRepository receptionRepository;
    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @InjectMocks
    private NotificationService notificationService;

    @org.junit.jupiter.api.BeforeEach
    void connecter() {
        com.backend.modules.auth.security.UtilisateurPrincipal principal =
                com.backend.modules.auth.security.UtilisateurPrincipal.builder()
                        .id(1L)
                        .email("acteur@labflow.test")
                        .password("x")
                        .nomComplet("Acteur Test")
                        .role(com.backend.modules.utilisateur.entity.RoleUtilisateur.SUPER_ADMINISTRATEUR)
                        .actif(true)
                        .permissions(java.util.Set.of())
                        .build();
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities()));
    }

    @org.junit.jupiter.api.AfterEach
    void deconnecter() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void publierPersisteLeStatutEnvoiee() {
        when(notificationRepository.save(any())).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setId(4L);
            return notification;
        });
        when(utilisateurRepository.findById(8L)).thenReturn(Optional.of(Utilisateur.builder()
                .id(8L)
                .nom("Bennani")
                .email("sara@labflow.ma")
                .motDePasseHash("hash")
                .build()));
        when(receptionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationDto reponse = notificationService.publier(NotificationDto.builder()
                .titre("Résultat disponible")
                .destinataireIds(List.of(8L))
                .canaux(List.of(CanalNotification.IN_APP))
                .build());

        ArgumentCaptor<NotificationReception> captor = ArgumentCaptor.forClass(NotificationReception.class);
        verify(receptionRepository).save(captor.capture());
        assertEquals(StatutEnvoi.ENVOYE, captor.getValue().getStatutEnvoi());
        assertEquals(CanalNotification.IN_APP, captor.getValue().getCanal());
        assertEquals(StatutEnvoi.ENVOYE, reponse.getStatut());
    }
}
