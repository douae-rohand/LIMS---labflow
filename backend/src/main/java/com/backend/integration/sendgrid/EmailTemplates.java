package com.backend.integration.sendgrid;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Templates HTML compatibles Gmail / Outlook (tables, styles inline).
 */
public final class EmailTemplates {

    private static final DateTimeFormatter DATE_FR = DateTimeFormatter
            .ofPattern("d MMMM yyyy 'à' HH:mm")
            .withLocale(Locale.FRENCH)
            .withZone(ZoneId.of("Africa/Casablanca"));

    private EmailTemplates() {
    }

    public static String confirmationDemande(String nomContact, String nomLaboratoire, String numero,
                                             String ville, int nombreDocuments, Instant dateSoumission) {
        return page(
                "Demande reçue",
                "Votre demande d'intégration a bien été enregistrée.",
                """
                <p>Bonjour %s,</p>
                <p>Nous avons bien reçu la demande d'intégration du laboratoire <strong>%s</strong>.</p>
                %s
                <p>Le Super Administrateur LabFlow va vérifier les informations et les documents transmis.
                Vous serez informé(e) dès qu'une décision sera prise. Aucun espace laboratoire n'est créé à ce stade.</p>
                """.formatted(
                        e(nomContact),
                        e(nomLaboratoire),
                        recap(numero, nomLaboratoire, ville, nombreDocuments, dateSoumission, "En cours de vérification", null)
                ),
                null,
                null
        );
    }

    public static String acceptation(String nomContact, String nomLaboratoire, String numero,
                                     String ville, Instant dateTraitement, String lienActivation, int ttlHeures) {
        String extra = lienActivation == null || lienActivation.isBlank()
                ? "<p>L'administrateur du laboratoire recevra séparément le lien pour activer son compte et définir son mot de passe.</p>"
                : """
                <p>Prochaine étape : activez le compte administrateur et choisissez votre mot de passe.
                Aucun mot de passe ne vous est envoyé par e-mail.</p>
                <p>Ce lien expire dans <strong>%d heures</strong> et ne peut être utilisé qu'une seule fois.</p>
                """.formatted(ttlHeures);
        return page(
                "Laboratoire accepté",
                "Le laboratoire " + nomLaboratoire + " a été intégré à LabFlow.",
                """
                <p>Bonjour %s,</p>
                <p>La demande d'intégration du laboratoire <strong>%s</strong> a été <strong>acceptée</strong>.</p>
                %s
                <p>L'environnement LabFlow du laboratoire a été créé (espace dédié).</p>
                %s
                """.formatted(
                        e(nomContact),
                        e(nomLaboratoire),
                        recap(numero, nomLaboratoire, ville, null, dateTraitement, "Acceptée", null),
                        extra
                ),
                lienActivation,
                lienActivation == null || lienActivation.isBlank() ? null : "Activer mon compte"
        );
    }

    public static String refus(String nomContact, String nomLaboratoire, String numero,
                               Instant dateTraitement, String motif) {
        return page(
                "Demande refusée",
                "La demande d'intégration n'a pas pu être acceptée.",
                """
                <p>Bonjour %s,</p>
                <p>La demande d'intégration du laboratoire <strong>%s</strong> a été <strong>refusée</strong>.</p>
                %s
                <p>Aucun espace laboratoire n'a été créé. Vous pouvez déposer une nouvelle demande après correction du dossier.</p>
                """.formatted(
                        e(nomContact),
                        e(nomLaboratoire),
                        recap(numero, nomLaboratoire, null, null, dateTraitement, "Refusée", motif)
                ),
                null,
                null
        );
    }

    public static String invitationAdministrateur(String nomComplet, String nomLaboratoire,
                                                  String lienActivation, int ttlHeures) {
        return page(
                "Activez votre compte",
                "Définissez votre mot de passe pour accéder à LabFlow.",
                """
                <p>Bonjour %s,</p>
                <p>Votre laboratoire <strong>%s</strong> a été intégré à LabFlow.
                Cliquez sur le bouton ci-dessous pour activer votre compte administrateur et choisir votre mot de passe.</p>
                <p>Ce lien expire dans <strong>%d heures</strong> et ne peut être utilisé qu'une seule fois.
                Aucun mot de passe ne vous est envoyé par e-mail.</p>
                """.formatted(e(nomComplet), e(nomLaboratoire), ttlHeures),
                lienActivation,
                "Activer mon compte"
        );
    }

    public static String codeOtp(String code, int ttlMinutes) {
        return page(
                "Code de vérification",
                "Utilisez ce code pour confirmer votre identité.",
                """
                <p>Votre code de vérification LabFlow :</p>
                <p style="font-size:28px;letter-spacing:6px;font-weight:700;color:#1A4A54;margin:16px 0;">%s</p>
                <p>Ce code expire dans <strong>%d minutes</strong>. S'il ne vient pas de vous, ignorez cet e-mail.</p>
                """.formatted(e(code), ttlMinutes),
                null,
                null
        );
    }

    public static String notificationDemande(String nomClient, String reference, String statut) {
        return page(
                "Mise à jour de demande",
                "Le statut de votre demande a changé.",
                """
                <p>Bonjour %s,</p>
                <p>Votre demande <strong>%s</strong> a changé de statut : <strong>%s</strong>.</p>
                <p>Connectez-vous à votre espace LabFlow pour plus de détails.</p>
                """.formatted(e(nomClient), e(reference), e(statut)),
                null,
                null
        );
    }

    public static String rapportDisponible(String nomClient, String reference, String lien) {
        return page(
                "Rapport disponible",
                "Votre rapport d'analyse est prêt.",
                """
                <p>Bonjour %s,</p>
                <p>Votre rapport d'analyse <strong>%s</strong> est prêt.</p>
                """.formatted(e(nomClient), e(reference)),
                lien,
                "Télécharger le rapport"
        );
    }

    public static String confirmationInscriptionClient(String nomComplet,
                                                       String lienActivation,
                                                       int ttlHeures) {
        return page(
                "Confirmez votre adresse e-mail",
                "Activez votre compte LabFlow en confirmant votre adresse.",
                """
                <p>Bonjour %s,</p>
                <p>Merci de votre inscription à LabFlow LIMS. Pour activer votre compte,
                confirmez votre adresse e-mail en cliquant sur le bouton ci-dessous.</p>
                <p>Ce lien expire dans <strong>%d heures</strong> et ne peut être utilisé
                qu'une seule fois. Si vous n'êtes pas à l'origine de cette inscription,
                ignorez simplement cet e-mail — aucun compte ne sera créé.</p>
                """.formatted(e(nomComplet), ttlHeures),
                lienActivation,
                "Confirmer mon adresse e-mail"
        );
    }

    public static String testConfiguration(String destinataire) {        return page(
                "Test SendGrid",
                "La configuration e-mail LabFlow fonctionne.",
                """
                <p>Cet e-mail de test a été envoyé à <strong>%s</strong> depuis l'application LIMS.</p>
                <p>Si vous le lisez, l'API SendGrid et l'expéditeur sont correctement configurés.</p>
                """.formatted(e(destinataire)),
                null,
                null
        );
    }

    public static String texteBrut(String html) {
        return html
                .replaceAll("(?s)<style.*?>.*?</style>", " ")
                .replaceAll("<br\\s*/?>", "\n")
                .replaceAll("</p>", "\n\n")
                .replaceAll("</tr>", "\n")
                .replaceAll("<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replaceAll("[ \\t]{2,}", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    static String e(String texte) {
        if (texte == null) {
            return "";
        }
        return texte
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static String recap(String numero, String nomLaboratoire, String ville,
                                Integer documents, Instant date, String statut, String motif) {
        StringBuilder rows = new StringBuilder();
        ligne(rows, "N° de demande", numero);
        ligne(rows, "Laboratoire", nomLaboratoire);
        if (ville != null && !ville.isBlank()) {
            ligne(rows, "Ville", ville);
        }
        if (documents != null) {
            ligne(rows, "Documents transmis", documents + " / 6");
        }
        if (date != null) {
            ligne(rows, "Date", DATE_FR.format(date));
        }
        ligne(rows, "Statut", statut);
        if (motif != null && !motif.isBlank()) {
            ligne(rows, "Motif", motif);
        }
        return """
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="margin:16px 0;border:1px solid #D7E4E2;border-radius:12px;">
                  %s
                </table>
                """.formatted(rows);
    }

    private static void ligne(StringBuilder rows, String label, String valeur) {
        rows.append("""
                <tr>
                  <td style="padding:10px 14px;font-size:13px;color:#5B6B70;width:40%%;border-bottom:1px solid #EEF3F2;">%s</td>
                  <td style="padding:10px 14px;font-size:13px;color:#1A2A2E;font-weight:600;border-bottom:1px solid #EEF3F2;">%s</td>
                </tr>
                """.formatted(e(label), e(valeur)));
    }

    private static String page(String preheader, String titre, String corps, String urlBouton, String libelleBouton) {
        String bouton = "";
        if (urlBouton != null && !urlBouton.isBlank() && libelleBouton != null) {
            bouton = """
                    <table role="presentation" cellspacing="0" cellpadding="0" style="margin:24px 0;">
                      <tr>
                        <td style="border-radius:999px;background:#1A4A54;">
                          <a href="%s" style="display:inline-block;padding:12px 22px;font-family:Arial,Helvetica,sans-serif;font-size:14px;font-weight:700;color:#ffffff;text-decoration:none;">%s</a>
                        </td>
                      </tr>
                    </table>
                    """.formatted(e(urlBouton), e(libelleBouton));
        }
        return """
                <!DOCTYPE html>
                <html lang="fr">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>%s</title>
                </head>
                <body style="margin:0;padding:0;background:#F4F7F6;">
                  <div style="display:none;max-height:0;overflow:hidden;opacity:0;">%s</div>
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#F4F7F6;padding:24px 0;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="600" cellspacing="0" cellpadding="0" style="width:600px;max-width:100%%;background:#ffffff;border-radius:16px;overflow:hidden;">
                          <tr>
                            <td style="background:#1A4A54;padding:22px 28px;">
                              <p style="margin:0;font-family:Arial,Helvetica,sans-serif;font-size:13px;letter-spacing:0.08em;color:#C8E86A;font-weight:700;">LABFLOW LIMS</p>
                              <h1 style="margin:8px 0 0;font-family:Arial,Helvetica,sans-serif;font-size:22px;line-height:1.3;color:#ffffff;">%s</h1>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:28px;font-family:Arial,Helvetica,sans-serif;font-size:15px;line-height:1.6;color:#1A2A2E;">
                              %s
                              %s
                              <p style="margin-top:28px;color:#5B6B70;font-size:13px;">L'équipe LabFlow</p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:16px 28px 24px;font-family:Arial,Helvetica,sans-serif;font-size:12px;color:#7A8A8E;">
                              Cet e-mail a été envoyé automatiquement. Merci de ne pas y répondre.
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(e(titre), e(preheader), e(titre), corps, bouton);
    }
}
