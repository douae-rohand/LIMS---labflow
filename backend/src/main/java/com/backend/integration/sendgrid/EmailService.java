package com.backend.integration.sendgrid;

import com.backend.integration.smtp.SmtpMailClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;

/**
 * Façade métier d'envoi d'e-mails : API SendGrid v3, avec repli sur SMTP ({@link SmtpMailClient}).
 * Tous les envois passent par {@link #envoyerHtml(List, String, String)}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final SendGridMailClient sendGridMailClient;
    private final SmtpMailClient smtpMailClient;

    /**
     * Point d'envoi unique : SendGrid d'abord, puis le SMTP de secours si SendGrid n'a pas
     * accepté le message (désactivé, non configuré, refus ou panne réseau).
     */
    public ResultatEnvoiEmail envoyerHtml(List<String> destinataires, String sujet, String corpsHtml) {
        String texte = EmailTemplates.texteBrut(corpsHtml);
        ResultatEnvoiEmail sendGrid = sendGridMailClient.envoyer(destinataires, sujet, corpsHtml, texte);
        if (sendGrid.accepte() || "ENVOI_IGNORE".equals(sendGrid.code()) || !smtpMailClient.actif()) {
            return sendGrid;
        }
        log.warn("SendGrid n'a pas envoyé l'e-mail [{}] ({}) : repli sur le SMTP de secours", sujet, sendGrid.code());
        ResultatEnvoiEmail smtp = smtpMailClient.envoyer(
                sendGridMailClient.normaliserDestinataires(destinataires), sujet, corpsHtml, texte);
        if (!smtp.accepte()) {
            log.error("E-mail [{}] non envoyé : SendGrid ({}) et SMTP ({}) ont échoué", sujet, sendGrid.code(), smtp.code());
            return sendGrid;
        }
        return smtp;
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

    public ResultatEnvoiEmail envoyerDemandeComplements(List<String> destinataires,
                                                        String nomContact,
                                                        String nomLaboratoire,
                                                        String numero,
                                                        String message,
                                                        List<String> documents,
                                                        String lien,
                                                        Instant expiration) {
        String sujet = "[LabFlow] Demande " + numero + " : compléments nécessaires";
        String html = EmailTemplates.demandeComplements(
                nomContact, nomLaboratoire, numero, message, documents, lien, expiration);
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

    public ResultatEnvoiEmail envoyerInvitationEmploye(String email, String nomComplet,
                                                        String nomLaboratoire,
                                                        String lienActivation, int ttlHeures) {
        String sujet = "[LabFlow] Activez votre compte " + nomLaboratoire;
        String html = EmailTemplates.invitationEmploye(
                nomComplet, nomLaboratoire, lienActivation, ttlHeures);
        return envoyerHtml(email, sujet, html);
    }

    @Async
    public void envoyerReinitMotDePasse(String email, String nomComplet,
                                        String lienReinit, int ttlMinutes) {
        String sujet = "[LabFlow] Réinitialisation de votre mot de passe";
        ResultatEnvoiEmail resultat = envoyerHtml(
                email, sujet,
                EmailTemplates.reinitMotDePasse(nomComplet, lienReinit, ttlMinutes));
        if (!resultat.accepte()) {
            // Jamais l'email, le lien ni le corps dans les logs
            log.warn("Envoi e-mail réinitialisation non accepté [code={}, temporaire={}]",
                    resultat.code(), resultat.temporaire());
        }
    }

    @Async
    public void envoyerConfirmationInscriptionClient(String email, String nomComplet,
                                                     String lienActivation, int ttlHeures) {
        String sujet = "[LabFlow] Confirmez votre adresse e-mail";
        ResultatEnvoiEmail resultat = envoyerHtml(
                email, sujet,
                EmailTemplates.confirmationInscriptionClient(nomComplet, lienActivation, ttlHeures));
        if (!resultat.accepte()) {
            // On ne journalise jamais l'email, le lien ni le corps — uniquement le code technique
            log.warn("Envoi confirmation inscription non accepté [code={}, temporaire={}] — " +
                     "l'utilisateur peut réessayer l'inscription pour obtenir un nouveau lien.",
                    resultat.code(), resultat.temporaire());
        }
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

    @Async
    public void envoyerDesactivationCompte(String email, String nomComplet, String nomLaboratoire) {
        String sujet = "[LabFlow] Désactivation de votre compte - " + nomLaboratoire;
        String html = EmailTemplates.desactivationCompte(nomComplet, nomLaboratoire);
        ResultatEnvoiEmail resultat = envoyerHtml(email, sujet, html);
        if (!resultat.accepte()) {
            log.warn("Envoi e-mail désactivation non accepté [code={}, temporaire={}]",
                    resultat.code(), resultat.temporaire());
        }
    }
}
