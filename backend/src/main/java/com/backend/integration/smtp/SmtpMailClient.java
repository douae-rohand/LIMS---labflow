package com.backend.integration.smtp;

import com.backend.integration.sendgrid.EmailTemplates;
import com.backend.integration.sendgrid.ResultatEnvoiEmail;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/**
 * Envoi d'e-mails par SMTP (ex. Gmail), utilisé en <b>secours</b> quand SendGrid n'a pas
 * accepté un message. Configuré par les variables {@code MAIL_*} du fichier {@code .env} ;
 * inactif tant que {@code MAIL_MAILER=smtp} et l'hôte, l'identifiant et le mot de passe
 * ne sont pas renseignés.
 */
@Slf4j
@Component
public class SmtpMailClient {

    private static final int DELAI_MS = 10_000;
    private static final long PAUSE_AVANT_NOUVEL_ESSAI_MS = 3_000;

    private final boolean actif;
    private final String expediteur;
    private final String nomExpediteur;
    private final JavaMailSenderImpl expediteurSmtp;

    public SmtpMailClient(
            @Value("${app.mail.smtp.mailer:}") String mailer,
            @Value("${app.mail.smtp.host:}") String hote,
            @Value("${app.mail.smtp.port:587}") int port,
            @Value("${app.mail.smtp.username:}") String identifiant,
            @Value("${app.mail.smtp.password:}") String motDePasse,
            @Value("${app.mail.smtp.encryption:tls}") String chiffrement,
            @Value("${app.mail.smtp.from-address:}") String adresseExpediteur,
            @Value("${app.mail.smtp.from-name:LabFlow LIMS}") String nomExpediteur) {
        this.actif = "smtp".equalsIgnoreCase(sansGuillemets(mailer))
                && StringUtils.hasText(hote) && StringUtils.hasText(identifiant) && StringUtils.hasText(motDePasse);
        String adresse = sansGuillemets(adresseExpediteur);
        this.expediteur = StringUtils.hasText(adresse) ? adresse : sansGuillemets(identifiant);
        String nom = sansGuillemets(nomExpediteur);
        this.nomExpediteur = StringUtils.hasText(nom) && !nom.contains("${") ? nom : "LabFlow LIMS";
        this.expediteurSmtp = actif ? construire(sansGuillemets(hote), port, sansGuillemets(identifiant),
                motDePasse.replace(" ", ""), sansGuillemets(chiffrement)) : null;
        if (actif) {
            log.info("SMTP de secours actif ({}:{})", sansGuillemets(hote), port);
        }
    }

    public boolean actif() {
        return actif;
    }

    /**
     * Envoie le message à des destinataires déjà validés. Gmail refuse parfois une connexion
     * passagèrement (« 421 Server busy », trop de connexions rapprochées) : une seconde
     * tentative est faite après une courte pause.
     */
    public ResultatEnvoiEmail envoyer(List<String> destinataires, String sujet, String html, String texte) {
        if (!actif) {
            return ResultatEnvoiEmail.nonConfigure("SMTP de secours non configuré (MAIL_MAILER=smtp)");
        }
        ResultatEnvoiEmail resultat = tenter(destinataires, sujet, html, texte);
        if (resultat.accepte()) {
            return resultat;
        }
        try {
            Thread.sleep(PAUSE_AVANT_NOUVEL_ESSAI_MS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return resultat;
        }
        log.info("SMTP : nouvelle tentative pour [{}]", sujet);
        return tenter(destinataires, sujet, html, texte);
    }

    private ResultatEnvoiEmail tenter(List<String> destinataires, String sujet, String html, String texte) {
        try {
            MimeMessage message = expediteurSmtp.createMimeMessage();
            MimeMessageHelper aide = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            aide.setFrom(new InternetAddress(expediteur, nomExpediteur, StandardCharsets.UTF_8.name()));
            aide.setTo(destinataires.toArray(String[]::new));
            aide.setSubject(sujet);
            aide.setText(StringUtils.hasText(texte) ? texte : EmailTemplates.texteBrut(html), html);
            expediteurSmtp.send(message);
            log.info("SMTP a envoyé l'e-mail [{}] à {} destinataire(s)", sujet, destinataires.size());
            return ResultatEnvoiEmail.accepteSmtp();
        } catch (MailAuthenticationException ex) {
            log.error("SMTP : authentification refusée (vérifiez MAIL_USERNAME / MAIL_PASSWORD)");
            return ResultatEnvoiEmail.echec(false, 0, "SMTP_AUTHENTIFICATION", "Le serveur SMTP a refusé l'identification.");
        } catch (MailSendException ex) {
            log.error("SMTP : envoi refusé pour [{}] : {}", sujet, ex.getMessage());
            return ResultatEnvoiEmail.echec(true, 0, "SMTP_ENVOI", "Le serveur SMTP n'a pas accepté le message.");
        } catch (MailException | jakarta.mail.MessagingException | java.io.UnsupportedEncodingException ex) {
            log.error("SMTP indisponible pour [{}] : {}", sujet, ex.getMessage());
            return ResultatEnvoiEmail.echec(true, 0, "SMTP_INDISPONIBLE", "Impossible de joindre le serveur SMTP.");
        }
    }

    /** Vérifie la connexion et l'identification SMTP, sans envoyer de message. */
    public String sonder() {
        if (!actif) {
            return "NON_CONFIGURE";
        }
        try {
            expediteurSmtp.testConnection();
            return "OK";
        } catch (Exception ex) {
            log.warn("Sonde SMTP indisponible");
            return "INDISPONIBLE";
        }
    }

    private static JavaMailSenderImpl construire(String hote, int port, String identifiant, String motDePasse,
                                                 String chiffrement) {
        JavaMailSenderImpl envoi = new JavaMailSenderImpl();
        envoi.setHost(hote);
        envoi.setPort(port);
        envoi.setUsername(identifiant);
        envoi.setPassword(motDePasse);
        envoi.setDefaultEncoding(StandardCharsets.UTF_8.name());
        boolean ssl = "ssl".equals(chiffrement.toLowerCase(Locale.ROOT)) || port == 465;
        envoi.setProtocol(ssl ? "smtps" : "smtp");
        String prefixe = ssl ? "mail.smtps." : "mail.smtp.";
        Properties proprietes = envoi.getJavaMailProperties();
        proprietes.put(prefixe + "auth", "true");
        proprietes.put(prefixe + "connectiontimeout", String.valueOf(DELAI_MS));
        proprietes.put(prefixe + "timeout", String.valueOf(DELAI_MS));
        proprietes.put(prefixe + "writetimeout", String.valueOf(DELAI_MS));
        if (!ssl && !"none".equalsIgnoreCase(chiffrement)) {
            proprietes.put("mail.smtp.starttls.enable", "true");
            proprietes.put("mail.smtp.starttls.required", "true");
        }
        return envoi;
    }

    private static String sansGuillemets(String valeur) {
        if (valeur == null) {
            return "";
        }
        String v = valeur.trim();
        if (v.length() >= 2 && (v.startsWith("\"") && v.endsWith("\"") || v.startsWith("'") && v.endsWith("'"))) {
            v = v.substring(1, v.length() - 1).trim();
        }
        return v;
    }
}
