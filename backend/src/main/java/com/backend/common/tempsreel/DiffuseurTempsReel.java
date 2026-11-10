package com.backend.common.tempsreel;

import com.backend.common.config.SchemaConstants;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Diffuse aux navigateurs connectés les « données modifiées » détectées après commit.
 * <p>
 * Les changements sont regroupés toutes les 300 ms (une action qui touche dix lignes ne produit
 * qu'un message) puis envoyés dans la file personnelle {@code /user/queue/evenements} de chaque
 * destinataire : les employés du laboratoire concerné, le client propriétaire de la demande,
 * et le Super Administrateur pour la plateforme. Le message ne contient que les domaines modifiés,
 * jamais de données : l'écran recharge lui-même ce qu'il affiche, avec ses droits habituels.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DiffuseurTempsReel {

    private static final Pattern SCHEMA = Pattern.compile("lims_[a-z0-9_]+");
    private static final String FILE = "/queue/evenements";

    /** Changement brut relevé par l'écouteur Hibernate. */
    record Changement(String domaine, String schema, Long laboratoireId, String refType, Long refId) {
    }

    private final SimpMessagingTemplate messagingTemplate;
    private final SimpUserRegistry utilisateursConnectes;
    private final JdbcTemplate jdbcTemplate;

    private final ConcurrentLinkedQueue<Changement> enAttente = new ConcurrentLinkedQueue<>();
    private final Map<String, Long> laboratoireParSchema = new ConcurrentHashMap<>();
    private ScheduledExecutorService planificateur;

    @PostConstruct
    void demarrer() {
        planificateur = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread fil = new Thread(r, "temps-reel");
            fil.setDaemon(true);
            return fil;
        });
        planificateur.scheduleWithFixedDelay(this::diffuser, 300, 300, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    void arreter() {
        planificateur.shutdownNow();
    }

    void signaler(Changement changement) {
        enAttente.add(changement);
    }

    private void diffuser() {
        if (enAttente.isEmpty()) {
            return;
        }
        try {
            Map<String, Set<String>> parUtilisateur = new HashMap<>();
            Map<Long, Set<String>> parLaboratoire = new HashMap<>();
            Set<String> plateforme = new HashSet<>();
            Changement c;
            while ((c = enAttente.poll()) != null) {
                Long laboratoire = c.laboratoireId() != null ? c.laboratoireId() : laboratoire(c.schema());
                if (laboratoire != null) {
                    parLaboratoire.computeIfAbsent(laboratoire, k -> new HashSet<>()).add(c.domaine());
                }
                if ("PLATEFORME".equals(c.domaine()) || "EMPLOYES".equals(c.domaine()) || "AUDIT".equals(c.domaine())) {
                    plateforme.add(c.domaine());
                }
                Long client = clientConcerne(c);
                if (client != null) {
                    parUtilisateur.computeIfAbsent(String.valueOf(client), k -> new HashSet<>()).add(c.domaine());
                }
            }
            for (SimpUser utilisateur : utilisateursConnectes.getUsers()) {
                if (!(utilisateur.getPrincipal() instanceof UtilisateurConnecte connecte)) {
                    continue;
                }
                Set<String> domaines = parUtilisateur.computeIfAbsent(connecte.getName(), k -> new HashSet<>());
                if (connecte.superAdministrateur()) {
                    domaines.addAll(plateforme);
                } else if (!connecte.client() && connecte.laboratoireId() != null) {
                    domaines.addAll(parLaboratoire.getOrDefault(connecte.laboratoireId(), Set.of()));
                }
            }
            parUtilisateur.forEach((utilisateur, domaines) -> {
                if (!domaines.isEmpty() && utilisateursConnectes.getUser(utilisateur) != null) {
                    messagingTemplate.convertAndSendToUser(utilisateur, FILE, Map.of("domaines", List.copyOf(domaines)));
                }
            });
        } catch (RuntimeException ex) {
            log.warn("Diffusion temps réel interrompue : {}", ex.getMessage());
        }
    }

    /** Utilisateur client propriétaire de la demande touchée (null si sans objet). */
    private Long clientConcerne(Changement c) {
        if (c.refType() == null || c.refId() == null || c.schema() == null || !SCHEMA.matcher(c.schema()).matches()) {
            return null;
        }
        String demande = switch (c.refType()) {
            case "DEMANDE" -> "?";
            case "FACTURE" -> "(SELECT f.demande_id FROM " + c.schema() + ".facture f WHERE f.id_facture = ?)";
            case "LIGNE" -> "(SELECT l.demande_id FROM " + c.schema() + ".ligne_essai l WHERE l.id_ligne_essai = ?)";
            default -> null;
        };
        if (demande == null) {
            return null;
        }
        List<Long> ids = jdbcTemplate.queryForList("SELECT c.utilisateur_id FROM " + c.schema() + ".demande d JOIN "
                + c.schema() + ".client c ON c.id_client = d.client_id WHERE d.id_demande = " + demande, Long.class, c.refId());
        return ids.isEmpty() ? null : ids.getFirst();
    }

    private Long laboratoire(String schema) {
        if (schema == null || !SCHEMA.matcher(schema).matches()) {
            return null;
        }
        return laboratoireParSchema.computeIfAbsent(schema, s -> jdbcTemplate.queryForList(
                "SELECT id_laboratoire FROM " + SchemaConstants.CENTRAL_SCHEMA + ".laboratoire WHERE nom_schema = ?",
                Long.class, s).stream().findFirst().orElse(null));
    }
}
