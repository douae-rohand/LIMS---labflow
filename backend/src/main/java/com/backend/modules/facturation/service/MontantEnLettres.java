package com.backend.modules.facturation.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Montant écrit en toutes lettres pour la mention « Arrêtée la présente facture à la somme de… »,
 * en français (règles de France / Maroc : soixante-dix, quatre-vingts, cent, mille…).
 */
public final class MontantEnLettres {

    private static final String[] UNITES = {"zéro", "un", "deux", "trois", "quatre", "cinq", "six", "sept", "huit",
            "neuf", "dix", "onze", "douze", "treize", "quatorze", "quinze", "seize", "dix-sept", "dix-huit", "dix-neuf"};
    private static final String[] DIZAINES = {"", "", "vingt", "trente", "quarante", "cinquante", "soixante"};

    private MontantEnLettres() {
    }

    /** Ex. 1250.50 → « mille deux cent cinquante dirhams et cinquante centimes ». */
    public static String dirhams(BigDecimal montant) {
        BigDecimal valeur = (montant == null ? BigDecimal.ZERO : montant).setScale(2, RoundingMode.HALF_UP).abs();
        long entier = valeur.longValue();
        int centimes = valeur.remainder(BigDecimal.ONE).movePointRight(2).intValue();
        // « un million de dirhams » : « de » après un nombre rond de millions.
        String unite = entier >= 1_000_000 && entier % 1_000_000 == 0 ? " de dirhams" : entier > 1 ? " dirhams" : " dirham";
        StringBuilder texte = new StringBuilder(nombre(entier)).append(unite);
        if (centimes > 0) {
            texte.append(" et ").append(nombre(centimes)).append(centimes > 1 ? " centimes" : " centime");
        }
        return texte.toString();
    }

    static String nombre(long n) {
        if (n < 0) {
            return "moins " + nombre(-n);
        }
        if (n < 1000) {
            return centaines((int) n, true);
        }
        if (n < 1_000_000) {
            long milliers = n / 1000;
            int reste = (int) (n % 1000);
            String tete = milliers == 1 ? "mille" : centaines((int) milliers, false) + " mille";
            return reste == 0 ? tete : tete + " " + centaines(reste, true);
        }
        long millions = n / 1_000_000;
        long reste = n % 1_000_000;
        String tete = nombre(millions) + (millions > 1 ? " millions" : " million");
        return reste == 0 ? tete : tete + " " + nombre(reste);
    }

    /** 0 à 999 ; {@code finale} : « cents » et « quatre-vingts » prennent un s en fin de nombre. */
    private static String centaines(int n, boolean finale) {
        if (n < 100) {
            return dizaines(n, finale);
        }
        int c = n / 100;
        int reste = n % 100;
        String tete = c == 1 ? "cent" : UNITES[c] + " cent" + (reste == 0 && finale ? "s" : "");
        return reste == 0 ? tete : tete + " " + dizaines(reste, finale);
    }

    private static String dizaines(int n, boolean finale) {
        if (n < 20) {
            return UNITES[n];
        }
        int d = n / 10;
        int u = n % 10;
        if (d == 7 || d == 9) {
            String base = d == 7 ? "soixante" : "quatre-vingt";
            int r = 10 + u;
            return base + (d == 7 && u == 1 ? " et " : "-") + UNITES[r];
        }
        if (d == 8) {
            return u == 0 ? "quatre-vingt" + (finale ? "s" : "") : "quatre-vingt-" + UNITES[u];
        }
        if (u == 0) {
            return DIZAINES[d];
        }
        return DIZAINES[d] + (u == 1 ? " et un" : "-" + UNITES[u]);
    }
}
