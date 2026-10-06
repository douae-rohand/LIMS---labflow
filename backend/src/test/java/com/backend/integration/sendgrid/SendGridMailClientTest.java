package com.backend.integration.sendgrid;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class SendGridMailClientTest {

    @Test
    void ignoreLesAdressesInvalidesEtDedoublonne() {
        SendGridMailClient client = nouveauClient(propertiesValides());
        assertEquals(
                List.of("admin@lab.ma", "contact@lab.ma"),
                client.normaliserDestinataires(List.of(
                        " Admin@Lab.ma ",
                        "pas-un-email",
                        "contact@lab.ma",
                        "admin@lab.ma",
                        " "
                )));
    }

    @Test
    void refuseUnPlaceholderDeCle() {
        SendGridProperties props = new SendGridProperties();
        props.setEnabled(true);
        props.setApiKey("SG.REMPLACER_PAR_VRAIE_CLE_SENDGRID");
        props.setFromEmail("no-reply@labflow.local");
        SendGridMailClient client = nouveauClient(props);

        ResultatEnvoiEmail resultat = client.envoyer(
                List.of("admin@lab.ma"), "Sujet", "<p>Test</p>", "Test");

        assertFalse(resultat.configure());
        assertEquals("SENDGRID_NON_CONFIGURE", resultat.code());
    }

    @Test
    void classe202CommeAccepteViaStatutOk() {
        SendGridMailClient client = nouveauClient(propertiesValides());
        ResultatEnvoiEmail resultat = client.classerErreur(400, "{\"errors\":[{\"message\":\"bad request\"}]}");
        assertFalse(resultat.accepte());
        assertFalse(resultat.temporaire());
        assertEquals("ERREUR_DEFINITIVE", resultat.code());
        assertEquals("bad request", resultat.message());
    }

    @Test
    void classe401CommeErreurDefinitive() {
        SendGridMailClient client = nouveauClient(propertiesValides());
        ResultatEnvoiEmail resultat = client.classerErreur(401, "{\"errors\":[{\"message\":\"Invalid API key\"}]}");
        assertFalse(resultat.accepte());
        assertFalse(resultat.temporaire());
        assertEquals("ERREUR_DEFINITIVE", resultat.code());
        assertEquals("Invalid API key", resultat.message());
    }

    @Test
    void classe429CommeErreurTemporaire() {
        SendGridMailClient client = nouveauClient(propertiesValides());
        ResultatEnvoiEmail resultat = client.classerErreur(429, "{\"errors\":[{\"message\":\"Too many requests\"}]}");
        assertTrue(resultat.temporaire());
        assertEquals("ERREUR_TEMPORAIRE", resultat.code());
    }

    @Test
    void echappeLeHtmlDansLesTemplates() {
        String html = EmailTemplates.refus(
                "<script>x</script>",
                "Lab \"A\"",
                "INT-1",
                null,
                "Dossier incomplet <b>oui</b>");
        assertTrue(html.contains("&lt;script&gt;x&lt;/script&gt;"));
        assertTrue(html.contains("Lab &quot;A&quot;"));
        assertTrue(html.contains("Dossier incomplet &lt;b&gt;oui&lt;/b&gt;"));
        assertFalse(html.contains("<script>x</script>"));
    }

    private static SendGridProperties propertiesValides() {
        SendGridProperties props = new SendGridProperties();
        props.setEnabled(true);
        props.setApiKey("SG.abcdefghijklmnopqrstuvwxyz0123456789");
        props.setFromEmail("no-reply@labflow.local");
        props.setFromName("LabFlow LIMS");
        return props;
    }

    private static SendGridMailClient nouveauClient(SendGridProperties props) {
        return new SendGridMailClient(mock(RestClient.class), props, new ObjectMapper());
    }
}
