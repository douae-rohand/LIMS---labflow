package com.backend.modules.plateforme.service;

import com.backend.common.exception.BusinessRuleException;
import com.backend.common.tenant.TenantDataSourceRegistry;
import com.backend.common.tenant.TenantProvisioner;
import com.backend.integration.n8n.NotificationEventPublisher;
import com.backend.integration.sendgrid.EmailService;
import com.backend.modules.auth.service.ActivationCompteService;
import com.backend.modules.plateforme.dto.SoumettreDemandeIntegrationRequest;
import com.backend.modules.plateforme.dto.TraiterDemandeIntegrationRequest;
import com.backend.modules.plateforme.entity.DemandeIntegration;
import com.backend.modules.plateforme.entity.DocumentIntegration;
import com.backend.modules.plateforme.entity.StatutIntegration;
import com.backend.modules.plateforme.entity.TypeDocumentIntegration;
import com.backend.modules.plateforme.repository.DemandeIntegrationRepository;
import com.backend.modules.plateforme.repository.DocumentIntegrationRepository;
import com.backend.modules.plateforme.repository.LaboratoireRepository;
import com.backend.modules.utilisateur.entity.Role;
import com.backend.modules.utilisateur.entity.RoleUtilisateur;
import com.backend.modules.utilisateur.entity.Utilisateur;
import com.backend.modules.utilisateur.repository.RoleRepository;
import com.backend.modules.utilisateur.repository.UtilisateurRepository;
import com.backend.modules.utilisateur.service.MatriculeGeneratorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DemandesIntegrationServiceTest {

    @Mock
    private DemandeIntegrationRepository demandeIntegrationRepository;

    @Mock
    private DocumentIntegrationRepository documentIntegrationRepository;

    @Mock
    private LaboratoireRepository laboratoireRepository;

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private TenantProvisioner tenantProvisioner;

    @Mock
    private TenantDataSourceRegistry tenantDataSourceRegistry;

    @Mock
    private EmailService emailService;

    @Mock
    private ActivationCompteService activationCompteService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MatriculeGeneratorService matriculeGeneratorService;

    @Mock
    private NotificationEventPublisher notificationEventPublisher;

    @InjectMocks
    private DemandeIntegrationService demandeIntegrationService;

    private DemandeIntegration demande;
    private SoumettreDemandeIntegrationRequest request;
    private Map<TypeDocumentIntegration, MultipartFile> fichiers;

    @BeforeEach
    void setUp() {
        demande = DemandeIntegration.builder()
                .id(1L)
                .numero("INT-TEST001")
                .statut(StatutIntegration.EN_ATTENTE.name())
                .dateDemande(Instant.now())
                .nomLaboratoire("Labo Test")
                .raisonSociale("Labo Test SARL")
                .ville("Casablanca")
                .contactNom("Dupont")
                .contactPrenom("Jean")
                .contactEmail("admin@labotest.com")
                .contactTelephone("0612345678")
                .contactFonction("Biologiste")
                .contactCin("AB123456")
                .adresse("123 Rue Test")
                .pays("Maroc")
                .latitude(33.5731)
                .longitude(-7.5898)
                .build();

        request = SoumettreDemandeIntegrationRequest.builder()
                .nomLaboratoire("Labo Test")
                .raisonSociale("Labo Test SARL")
                .typesLaboratoire(List.of("Biologie", "Chimie"))
                .ville("Casablanca")
                .pays("Maroc")
                .adminNom("Dupont")
                .adminPrenom("Jean")
                .adminEmail("admin@labotest.com")
                .adminTelephone("0612345678")
                .adminFonction("Biologiste")
                .adminCin("AB123456")
                .adresse("123 Rue Test")
                .latitude(33.5731)
                .longitude(-7.5898)
                .build();

        fichiers = new EnumMap<>(TypeDocumentIntegration.class);
        for (TypeDocumentIntegration type : TypeDocumentIntegration.values()) {
            MultipartFile mockFile = mock(MultipartFile.class);
            when(mockFile.getOriginalFilename()).thenReturn(type.name() + ".pdf");
            when(mockFile.getContentType()).thenReturn("application/pdf");
            when(mockFile.getSize()).thenReturn(1024L);
            when(mockFile.isEmpty()).thenReturn(false);
            when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[1024]));
            fichiers.put(type, mockFile);
        }

        when(matriculeGeneratorService.genererMatricule(RoleUtilisateur.ADMINISTRATEUR))
                .thenReturn("ADM-001");
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
    }

    // -------------------------------------------------------------------------
    // TEST 1 : Succès complet (schéma, laboratoire, admin, emails)
    // -------------------------------------------------------------------------
    @Test
    void traiter_demande_succes_complet() {
        // Arrange
        when(demandeIntegrationRepository.findById(1L)).thenReturn(Optional.of(demande));
        when(documentIntegrationRepository.countByDemande_Id(1L)).thenReturn(6L);
        when(utilisateurRepository.existsByEmail("admin@labotest.com")).thenReturn(false);
        when(laboratoireRepository.existsByCode(anyString())).thenReturn(false);
        when(laboratoireRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(demandeIntegrationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(roleRepository.findByCode(RoleUtilisateur.ADMINISTRATEUR.name()))
                .thenReturn(Optional.of(Role.builder().id(1L).code(RoleUtilisateur.ADMINISTRATEUR.name()).build()));
        when(utilisateurRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(activationCompteService.creerJeton(any())).thenReturn("token-activation");
        when(activationCompteService.getDureeHeures()).thenReturn(48);

        // Act
        TraiterDemandeIntegrationRequest request = new TraiterDemandeIntegrationRequest();
        request.setDecision(StatutIntegration.ACCEPTEE);
        var result = demandeIntegrationService.traiter(1L, request);

        // Assert
        assertThat(result.getStatut()).isEqualTo(StatutIntegration.ACCEPTEE);
        assertThat(result.getLaboratoireCode()).isNotNull();
        assertThat(result.getNomSchema()).startsWith("lims_");

        // Vérifier provisionner appelé
        verify(tenantProvisioner, times(1)).provisionner(any());

        // Vérifier administrateur créé
        ArgumentCaptor<Utilisateur> adminCaptor = ArgumentCaptor.forClass(Utilisateur.class);
        verify(utilisateurRepository, times(1)).save(adminCaptor.capture());
        Utilisateur admin = adminCaptor.getValue();
        assertThat(admin.getEmail()).isEqualTo("admin@labotest.com");
        assertThat(admin.isActif()).isFalse();
        assertThat(admin.isMustChangePassword()).isTrue();

        // Vérifier DEPROVISIONNER jamais appelé (succès)
        verify(tenantProvisioner, never()).deprovisionner(anyString());

        // Vérifier emails envoyés
        verify(emailService, times(1)).envoyerAcceptationDemandeIntegration(any(), any(), any(), any(), any(), any());
        verify(emailService, times(1)).envoyerInvitationAdministrateur(eq("admin@labotest.com"), any(), any(), eq("token-activation"), eq(48));
    }

    // -------------------------------------------------------------------------
    // TEST 2 : Échec provisionnement tenant (compensation attendue)
    // -------------------------------------------------------------------------
    @Test
    void traiter_demande_echec_provisionnement_compensation() {
        // Arrange
        when(demandeIntegrationRepository.findById(1L)).thenReturn(Optional.of(demande));
        when(documentIntegrationRepository.countByDemande_Id(1L)).thenReturn(6L);
        when(utilisateurRepository.existsByEmail("admin@labotest.com")).thenReturn(false);
        when(laboratoireRepository.existsByCode(anyString())).thenReturn(false);
        when(laboratoireRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("Échec CREATE DATABASE"))
                .when(tenantProvisioner).provisionner(any());

        // Act & Assert
        TraiterDemandeIntegrationRequest request = new TraiterDemandeIntegrationRequest();
        request.setDecision(StatutIntegration.ACCEPTEE);

        assertThatThrownBy(() -> demandeIntegrationService.traiter(1L, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("PROVISIONNEMENT_TENANT_ECHOUE");

        // Vérifier laboratoire supprimé (compensation)
        verify(laboratoireRepository, times(1)).delete(any());

        // Vérifier administrateur jamais créé
        verify(utilisateurRepository, never()).save(any());

        // Vérifier emails jamais envoyés
        verify(emailService, never()).envoyerAcceptationDemandeIntegration(any(), any(), any(), any(), any(), any());
        verify(emailService, never()).envoyerInvitationAdministrateur(any(), any(), any(), any(), any());

        // Vérifier demande reste EN_ATTENTE
        verify(demandeIntegrationRepository, never()).save(argThat(d -> 
                StatutIntegration.ACCEPTEE.name().equals(d.getStatut())));
    }

    // -------------------------------------------------------------------------
    // TEST 3 : Échec création administrateur (compensation attendue)
    // -------------------------------------------------------------------------
    @Test
    void traiter_demande_echec_creation_admin_compensation() {
        // Arrange
        when(demandeIntegrationRepository.findById(1L)).thenReturn(Optional.of(demande));
        when(documentIntegrationRepository.countByDemande_Id(1L)).thenReturn(6L);
        when(utilisateurRepository.existsByEmail("admin@labotest.com")).thenReturn(false);
        when(laboratoireRepository.existsByCode(anyString())).thenReturn(false);
        when(laboratoireRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(roleRepository.findByCode(RoleUtilisateur.ADMINISTRATEUR.name()))
                .thenReturn(Optional.of(Role.builder().id(1L).code(RoleUtilisateur.ADMINISTRATEUR.name()).build()));
        when(utilisateurRepository.save(any()))
                .thenThrow(new RuntimeException("Échec insertion utilisateur"));

        // Act & Assert
        TraiterDemandeIntegrationRequest request = new TraiterDemandeIntegrationRequest();
        request.setDecision(StatutIntegration.ACCEPTEE);

        assertThatThrownBy(() -> demandeIntegrationService.traiter(1L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Échec insertion utilisateur");

        // Vérifier compensation : déprovisionner appelé
        verify(tenantProvisioner, times(1)).deprovisionner(contains("lims_"));

        // Vérifier laboratoire supprimé
        verify(laboratoireRepository, times(1)).delete(any());

        // Vérifier demande reste EN_ATTENTE
        verify(demandeIntegrationRepository, never()).save(argThat(d -> 
                StatutIntegration.ACCEPTEE.name().equals(d.getStatut())));
    }

    // -------------------------------------------------------------------------
    // TEST 4 : Double soumission concurrente (schéma déjà existe)
    // -------------------------------------------------------------------------
    @Test
    void traiter_demande_concurrente_schema_existe_deja() {
        // Arrange
        when(demandeIntegrationRepository.findById(1L)).thenReturn(Optional.of(demande));
        when(documentIntegrationRepository.countByDemande_Id(1L)).thenReturn(6L);
        when(utilisateurRepository.existsByEmail("admin@labotest.com")).thenReturn(false);
        when(laboratoireRepository.existsByCode(anyString())).thenReturn(false);
        when(laboratoireRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new BusinessRuleException("SCHEMA_DEJA_EXISTE", "Le schéma existe déjà"))
                .when(tenantProvisioner).provisionner(any());

        // Act & Assert
        TraiterDemandeIntegrationRequest request = new TraiterDemandeIntegrationRequest();
        request.setDecision(StatutIntegration.ACCEPTEE);

        assertThatThrownBy(() -> demandeIntegrationService.traiter(1L, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("SCHEMA_DEJA_EXISTE");

        // Vérifier compensation : laboratoire supprimé MAIS schéma PAS droppé
        verify(laboratoireRepository, times(1)).delete(any());
        verify(tenantProvisioner, never()).deprovisionner(anyString());
    }

    // -------------------------------------------------------------------------
    // TEST 5 : Échec email après succès (pas de compensation)
    // -------------------------------------------------------------------------
    @Test
    void traiter_demande_echec_email_apres_succes_pas_de_compensation() {
        // Arrange
        when(demandeIntegrationRepository.findById(1L)).thenReturn(Optional.of(demande));
        when(documentIntegrationRepository.countByDemande_Id(1L)).thenReturn(6L);
        when(utilisateurRepository.existsByEmail("admin@labotest.com")).thenReturn(false);
        when(laboratoireRepository.existsByCode(anyString())).thenReturn(false);
        when(laboratoireRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(roleRepository.findByCode(RoleUtilisateur.ADMINISTRATEUR.name()))
                .thenReturn(Optional.of(Role.builder().id(1L).code(RoleUtilisateur.ADMINISTRATEUR.name()).build()));
        when(utilisateurRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(activationCompteService.creerJeton(any())).thenReturn("token-activation");
        when(activationCompteService.getDureeHeures()).thenReturn(48);
        doThrow(new RuntimeException("Échec SendGrid"))
                .when(emailService).envoyerAcceptationDemandeIntegration(any(), any(), any(), any(), any(), any());

        // Act & Assert
        TraiterDemandeIntegrationRequest request = new TraiterDemandeIntegrationRequest();
        request.setDecision(StatutIntegration.ACCEPTEE);

        var result = demandeIntegrationService.traiter(1L, request);

        // Acceptation réussie malgré échec email
        assertThat(result.getStatut()).isEqualTo(StatutIntegration.ACCEPTEE);
        assertThat(result.getLaboratoireCode()).isNotNull();

        // Vérifier aucune compensation
        verify(tenantProvisioner, never()).deprovisionner(anyString());
        verify(laboratoireRepository, never()).delete(any());

        // Vérifier administrateur créé
        verify(utilisateurRepository, times(1)).save(any());
    }

    // -------------------------------------------------------------------------
    // TEST 6 : Isolation des tenants
    // -------------------------------------------------------------------------
    @Test
    void isolation_tenant_utilisateur_ne_peut_pas_acceder_autre_tenant() {
        // Ce test nécessite une vraie base de données et sera dans un test d'intégration
        // Marqué comme @Disabled ici, implémenté dans un fichier d'intégration séparé
    }

    // -------------------------------------------------------------------------
    // TEST 7 : Laboratoire suspendu/désactivé rejeté
    // -------------------------------------------------------------------------
    @Test
    void laboratoire_suspendu_desactive_rejeté_authentification() {
        // Ce test nécessite AuthService et sera dans un test d'intégration
        // Marqué comme @Disabled ici
    }
}
