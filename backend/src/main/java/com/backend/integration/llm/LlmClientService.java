package com.backend.integration.llm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import com.openai.errors.RateLimitException;
import com.openai.errors.UnauthorizedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Client du modèle de langage (Groq, via l'API compatible OpenAI de Spring AI).
 *
 * <p>Ne renvoie jamais un texte d'erreur à la place d'une réponse : en cas d'échec,
 * {@link IaIndisponibleException} est levée et traduite en HTTP 503 (ou 429 si le
 * quota du fournisseur est atteint).
 */
@Slf4j
@Service
public class LlmClientService {

    private final ChatClient chatClient;
    private final String modele;

    public LlmClientService(ChatClient.Builder chatClientBuilder,
                            @Value("${spring.ai.openai.chat.model:inconnu}") String modele) {
        this.chatClient = chatClientBuilder.build();
        this.modele = modele;
    }

    /**
     * Envoie un message système et un message utilisateur, renvoie le texte produit.
     *
     * @throws IaIndisponibleException si le fournisseur refuse, est saturé ou ne répond pas
     */
    public String repondre(String systeme, String utilisateur) {
        return repondre(systeme, utilisateur, null);
    }

    /**
     * Comme {@link #repondre(String, String)}, en laissant le modèle appeler les méthodes {@code @Tool}
     * de {@code outils} (lecture des données du laboratoire). Les outils sont exécutés par Spring AI dans
     * ce même fil, avant la réponse finale.
     */
    public String repondre(String systeme, String utilisateur, Object outils) {
        long debut = System.currentTimeMillis();
        try {
            var requete = chatClient.prompt()
                    .system(systeme)
                    .user(utilisateur);
            if (outils != null) {
                // Groq refuse, au second tour, le champ « reasoning_content » que Spring AI recopie depuis
                // la première réponse : on demande à Groq de ne pas renvoyer le raisonnement du modèle.
                requete = requete.tools(outils)
                        .options(org.springframework.ai.openai.OpenAiChatOptions.builder()
                                .extraBody(java.util.Map.of("include_reasoning", false)));
            }
            String contenu = requete
                    .call()
                    .content();
            if (!StringUtils.hasText(contenu)) {
                throw new IaIndisponibleException(false, "L'assistant n'a pas pu formuler de réponse. Reformulez votre question.");
            }
            log.debug("Réponse LLM ({}) en {} ms", modele, System.currentTimeMillis() - debut);
            return contenu.trim();
        } catch (IaIndisponibleException ex) {
            throw ex;
        } catch (Exception ex) {
            if (cause(ex, RateLimitException.class)) {
                log.warn("Quota du fournisseur LLM atteint ({})", modele);
                throw new IaIndisponibleException(true, "L'assistant est très sollicité. Réessayez dans une minute.");
            }
            if (cause(ex, UnauthorizedException.class)) {
                log.error("Clé API du fournisseur LLM refusée : vérifier LLM_API_KEY");
            } else {
                log.error("Appel LLM échoué ({}) : {}", modele, premiereLigne(ex.getMessage()));
            }
            throw new IaIndisponibleException(false, "L'assistant est momentanément indisponible.");
        }
    }

    /** Nom du modèle configuré (ex. {@code llama-3.3-70b-versatile}). */
    public String getModeleActif() {
        return modele;
    }

    private static boolean cause(Throwable ex, Class<? extends Throwable> type) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (type.isInstance(t)) return true;
            if (t.getCause() == t) break;
        }
        return false;
    }

    /** Évite de recopier dans les logs un corps de réponse entier. */
    private static String premiereLigne(String texte) {
        if (texte == null) return "";
        String ligne = texte.lines().findFirst().orElse("");
        return ligne.length() > 300 ? ligne.substring(0, 300) + "…" : ligne;
    }
}
