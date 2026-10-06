package com.backend.integration.sendgrid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Façade métier d'envoi d'e-mails via l'API SendGrid v3.
 * Un seul mécanisme d'envoi : {@link SendGridMailClient}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final SendGridMailClient sendGridMailClient;

    public ResultatEnvoiEmail envoyerHtml(List<String> destinataires, String sujet, String corpsHtml) {
        return sendGridMailClient.envoyer(destinataires, sujet, corpsHtml, EmailTemplates.texteBrut(corpsHtml));
    }

    public ResultatEnvoiEmail envoyerHtml(String destinataire, String sujet, String corpsHtml) {
        return envoyerHtml(destinataire == null ? List.of() : List.of(destinataire), sujet, corpsHtml);
    }

    @Async
    public void envoyerHtmlAsync(List<String> destinataires, String sujet, String corpsHtml) {
        envoyerHtml(destinataires, sujet, corpsHtml);
    }

    public ResultatEnvoiEmail envoyerConfirmationDemandeIntegration(List<String> destinataires,
                                                                   String nomContact,
                                                                   String nomLaboratoire,
                                                                   String numero,
                                                                   String ville,
                                                                   int nombreDocuments,
                                                                   Instant dateSoumission) {
        String sujet = "[LabFlow] Demande " + numero + " reçue";
        String html = EmailTemplates.confirmationDemande(
                nomContact, nomLaboratoire, numero, ville, nombreDocuments, dateSoumission);
        return envoyerHtml(destinataires, sujet, html);
    }

    public ResultatEnvoiEmail envoyerAcceptationDemandeIntegration(List<String> destinataires,
                                                                  String nomContact,
                                                                  String nomLaboratoire,
                                                                  String numero,
                                                                  String ville,
                                                                  Instant dateTraitement) {
        String sujet = "[LabFlow] Demande " + numero + " acceptée";
        String html = EmailTemplates.acceptation(
                nomContact, nomLaboratoire, numero, ville, dateTraitement, null, 0);
        return envoyerHtml(destinataires, sujet, html);
    }

    public ResultatEnvoiEmail envoyerRefusDemandeIntegration(List<String> destinataires,
                                                             String nomContact,
                                                             String nomLaboratoire,
                                                             String numero,
                                                             Instant dateTraitement,
                                                             String motif) {
        String sujet = "[LabFlow] Demande " + numero + " refusée";
        String html = EmailTemplates.refus(nomContact, nomLaboratoire, numero, dateTraitement, motif);
        return envoyerHtml(destinataires, sujet, html);
    }

    public ResultatEnvoiEmail envoyerInvitationAdministrateur(String email, String nomComplet,
                                                              String nomLaboratoire,
                                                              String lienActivation, int ttlHeures) {
        String sujet = "[LabFlow] Activez le compte administrateur de " + nomLaboratoire;
        String html = EmailTemplates.invitationAdministrateur(
                nomComplet, nomLaboratoire, lienActivation, ttlHeures);
        return envoyerHtml(email, sujet, html);
    }

    public ResultatEnvoiEmail envoyerTest(String destinataire) {
        String sujet = "[LabFlow] Test de configuration SendGrid";
        return envoyerHtml(destinataire, sujet, EmailTemplates.testConfiguration(destinataire));
    }

    @Async
    public void envoyerNotificationDemande(String email, String nomClient,
                                           String referenceDemande, String statut) {
        String sujet = "[LabFlow] Demande " + referenceDemande + " – " + statut;
        envoyerHtml(email, sujet, EmailTemplates.notificationDemande(nomClient, referenceDemande, statut));
    }

    @Async
    public void envoyerRapportDisponible(String email, String nomClient,
                                         String referenceRapport, String lienRapport) {
        String sujet = "[LabFlow] Votre rapport " + referenceRapport + " est disponible";
        envoyerHtml(email, sujet, EmailTemplates.rapportDisponible(nomClient, referenceRapport, lienRapport));
    }

    @Async
    public void envoyerCodeOtp(String email, String code, int ttlMinutes) {
        String sujet = "[LabFlow] Code de vérification";
        envoyerHtml(email, sujet, EmailTemplates.codeOtp(code, ttlMinutes));
    }
}
