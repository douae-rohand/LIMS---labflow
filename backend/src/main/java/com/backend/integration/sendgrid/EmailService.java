package com.backend.integration.sendgrid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.List;

/**
 * Service d'envoi d'emails via SendGrid SMTP relay (spring-boot-starter-mail).
 *
 * <p>La configuration SMTP pointe sur {@code smtp.sendgrid.net:587} avec
 * {@code username=apikey} et {@code password=<SG.xxx>} dans application.yaml.
 *
 * <p>Tous les envois sont asynchrones pour ne pas bloquer le thread HTTP.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:no-reply@lims.local}")
    private String expediteurDefaut;

    // -------------------------------------------------------------------------
    // Envoi simple (texte brut)
    // -------------------------------------------------------------------------

    @Async
    public void envoyerSimple(String destinataire, String sujet, String corps) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(expediteurDefaut);
            message.setTo(destinataire);
            message.setSubject(sujet);
            message.setText(corps);
            mailSender.send(message);
            log.info("Email simple envoyé à : {}", destinataire);
        } catch (MailException ex) {
            log.error("Erreur envoi email simple à {} : {}", destinataire, ex.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Envoi HTML
    // -------------------------------------------------------------------------

    @Async
    public void envoyerHtml(String destinataire, String sujet, String corpsHtml) {
        envoyerHtml(List.of(destinataire), sujet, corpsHtml);
    }

    @Async
    public void envoyerHtml(List<String> destinataires, String sujet, String corpsHtml) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(expediteurDefaut);
            helper.setTo(destinataires.toArray(new String[0]));
            helper.setSubject(sujet);
            helper.setText(corpsHtml, true);
            mailSender.send(message);
            log.info("Email HTML envoyé à {} destinataire(s)", destinataires.size());
        } catch (MessagingException | MailException ex) {
            log.error("Erreur envoi email HTML : {}", ex.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Templates métier
    // -------------------------------------------------------------------------

    @Async
    public void envoyerNotificationDemande(String email, String nomClient,
                                            String referenceDemande, String statut) {
        String sujet = "[LIMS] Demande " + referenceDemande + " – " + statut;
        String corps = String.format("""
                <html><body>
                <p>Bonjour %s,</p>
                <p>Votre demande <strong>%s</strong> a changé de statut : <strong>%s</strong>.</p>
                <p>Connectez-vous à votre espace LIMS pour plus de détails.</p>
                <br><p>L'équipe LIMS</p>
                </body></html>
                """, nomClient, referenceDemande, statut);
        envoyerHtml(email, sujet, corps);
    }

    @Async
    public void envoyerRapportDisponible(String email, String nomClient,
                                          String referenceRapport, String lienRapport) {
        String sujet = "[LIMS] Votre rapport " + referenceRapport + " est disponible";
        String corps = String.format("""
                <html><body>
                <p>Bonjour %s,</p>
                <p>Votre rapport d'analyse <strong>%s</strong> est prêt.</p>
                <p><a href="%s">Télécharger le rapport</a></p>
                <br><p>L'équipe LIMS</p>
                </body></html>
                """, nomClient, referenceRapport, lienRapport);
        envoyerHtml(email, sujet, corps);
    }

    @Async
    public void envoyerCodeOtp(String email, String code, int ttlMinutes) {
        String sujet = "[LIMS] Code de vérification";
        String corps = String.format("""
                <html><body>
                <p>Votre code de vérification est : <strong style="font-size:24px">%s</strong></p>
                <p>Ce code expire dans %d minutes.</p>
                <p>Si vous n'avez pas demandé ce code, ignorez cet email.</p>
                </body></html>
                """, code, ttlMinutes);
        envoyerHtml(email, sujet, corps);
    }
}
