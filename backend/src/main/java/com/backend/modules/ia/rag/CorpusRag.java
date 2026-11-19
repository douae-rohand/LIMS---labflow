package com.backend.modules.ia.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lit la documentation de l'assistant ({@code resources/rag/*.md}) et la découpe en passages :
 * un passage par titre de niveau 2 ({@code ##}).
 *
 * <p>La première ligne de chaque page indique à qui elle s'adresse :
 * {@code <!-- audience: tous -->}. Valeurs : {@code tous}, {@code public}, {@code client},
 * {@code personnel}, {@code accueil}, {@code responsable}, {@code technicien},
 * {@code administrateur}, {@code super-admin}. Sans cette ligne, la page vaut {@code tous}.
 */
@Slf4j
@Component
public class CorpusRag {

    /** Au-delà, un passage est coupé en morceaux pour rester précis lors de la recherche. */
    private static final int TAILLE_MAX = 1500;
    private static final Pattern AUDIENCE = Pattern.compile("^\\s*<!--\\s*audience:\\s*([a-z-]+)\\s*-->");

    public record Passage(String source, String audience, String titre, String texte) {}

    public List<Passage> lire() {
        List<Passage> passages = new ArrayList<>();
        try {
            Resource[] fichiers = new PathMatchingResourcePatternResolver().getResources("classpath*:rag/*.md");
            Arrays.sort(fichiers, Comparator.comparing(r -> String.valueOf(r.getFilename())));
            for (Resource fichier : fichiers) {
                String contenu = fichier.getContentAsString(StandardCharsets.UTF_8);
                passages.addAll(decouper(String.valueOf(fichier.getFilename()), contenu));
            }
        } catch (IOException ex) {
            log.error("Lecture du corpus de l'assistant impossible : {}", ex.getMessage());
        }
        return passages;
    }

    static List<Passage> decouper(String source, String contenu) {
        String audience = ProfilsAssistantService.AUDIENCE_TOUS;
        Matcher m = AUDIENCE.matcher(contenu);
        if (m.find()) {
            audience = m.group(1);
            contenu = contenu.substring(m.end());
        }
        List<Passage> passages = new ArrayList<>();
        String titreDocument = source;
        String titreSection = null;
        StringBuilder corps = new StringBuilder();
        for (String ligne : contenu.split("\\R")) {
            if (ligne.startsWith("# ")) {
                titreDocument = ligne.substring(2).trim();
            } else if (ligne.startsWith("## ")) {
                ajouter(passages, source, audience, titreDocument, titreSection, corps);
                titreSection = ligne.substring(3).trim();
                corps.setLength(0);
            } else {
                corps.append(ligne).append('\n');
            }
        }
        ajouter(passages, source, audience, titreDocument, titreSection, corps);
        return passages;
    }

    private static void ajouter(List<Passage> passages, String source, String audience, String titreDocument,
                                String titreSection, StringBuilder corps) {
        String texte = corps.toString().trim();
        if (texte.isEmpty()) return;
        String titre = titreSection == null ? titreDocument : titreDocument + " – " + titreSection;
        // Découpe par paragraphes si la section est trop longue
        StringBuilder morceau = new StringBuilder();
        for (String paragraphe : texte.split("\\n\\s*\\n")) {
            if (morceau.length() > 0 && morceau.length() + paragraphe.length() > TAILLE_MAX) {
                passages.add(new Passage(source, audience, titre, titre + "\n" + morceau.toString().trim()));
                morceau.setLength(0);
            }
            morceau.append(paragraphe).append("\n\n");
        }
        if (morceau.length() > 0) {
            passages.add(new Passage(source, audience, titre, titre + "\n" + morceau.toString().trim()));
        }
    }
}
