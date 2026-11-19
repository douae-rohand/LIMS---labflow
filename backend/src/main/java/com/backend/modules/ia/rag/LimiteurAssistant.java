package com.backend.modules.ia.rag;

import com.backend.common.exception.TropDeRequetesException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limite le nombre de questions posées à l'assistant sur une heure glissante.
 * La clé identifie l'auteur : {@code u:<id>} pour un utilisateur connecté,
 * {@code ip:<adresse>} pour un visiteur. Protège le quota gratuit du fournisseur (Groq).
 */
@Component
public class LimiteurAssistant {

    private static final Duration FENETRE = Duration.ofHours(1);

    private final Map<String, Deque<Instant>> questions = new ConcurrentHashMap<>();

    /** Enregistre une question ; lève une 429 si la limite est atteinte. */
    public void consommer(String cle, int maxParHeure) {
        Instant maintenant = Instant.now();
        Deque<Instant> historique = questions.computeIfAbsent(cle, c -> new ArrayDeque<>());
        synchronized (historique) {
            purger(historique, maintenant);
            if (historique.size() >= maxParHeure) {
                long minutes = Math.max(1, Duration.between(maintenant, historique.peekFirst().plus(FENETRE)).toMinutes());
                throw new TropDeRequetesException("Vous avez posé " + maxParHeure
                        + " questions en une heure. Vous pourrez en poser une nouvelle dans " + minutes + " min.");
            }
            historique.addLast(maintenant);
        }
    }

    /** Ménage périodique des auteurs inactifs. */
    @Scheduled(fixedDelay = 15 * 60 * 1000L)
    public void nettoyer() {
        Instant maintenant = Instant.now();
        questions.entrySet().removeIf(entree -> {
            synchronized (entree.getValue()) {
                purger(entree.getValue(), maintenant);
                return entree.getValue().isEmpty();
            }
        });
    }

    private static void purger(Deque<Instant> historique, Instant maintenant) {
        while (!historique.isEmpty() && historique.peekFirst().plus(FENETRE).isBefore(maintenant)) {
            historique.pollFirst();
        }
    }
}
