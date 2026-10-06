package com.backend.integration.sendgrid;

/**
 * Résultat d'un appel SendGrid, sans jamais exposer la clé API.
 */
public record ResultatEnvoiEmail(
        boolean accepte,
        boolean temporaire,
        boolean configure,
        int statutHttp,
        String code,
        String message
) {
    public static ResultatEnvoiEmail nonConfigure(String message) {
        return new ResultatEnvoiEmail(false, false, false, 0, "SENDGRID_NON_CONFIGURE", message);
    }

    public static ResultatEnvoiEmail ignore(String message) {
        return new ResultatEnvoiEmail(false, false, false, 0, "ENVOI_IGNORE", message);
    }

    public static ResultatEnvoiEmail accepte(int statutHttp) {
        return new ResultatEnvoiEmail(true, false, true, statutHttp, "ACCEPTE",
                "E-mail accepté par SendGrid");
    }

    public static ResultatEnvoiEmail echec(boolean temporaire, int statutHttp, String code, String message) {
        return new ResultatEnvoiEmail(false, temporaire, true, statutHttp, code, message);
    }
}
