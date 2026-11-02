package com.backend.common.audit;

import com.backend.modules.utilisateur.entity.Utilisateur;
import com.backend.modules.utilisateur.repository.UtilisateurRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JournalAuditService {

    private final JournalAuditRepository journalAuditRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ObjectMapper objectMapper;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enregistrer(JournalAudit entree) {
        try {
            journalAuditRepository.save(entree);
        } catch (Exception ex) {
            log.error("Erreur lors de la persistance de l'audit [action={}] : {}",
                    entree.getAction(), ex.getMessage());
        }
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enregistrer(String action, String nomEntite, Long idEntite,
                            String utilisateur, String description) {
        Utilisateur auteur = utilisateurRepository.findByEmail(utilisateur).orElse(null);
        if (auteur == null) {
            log.warn("Audit ignoré : utilisateur introuvable pour {}", utilisateur);
            return;
        }
        String objet = idEntite == null ? nomEntite : nomEntite + "#" + idEntite;
        JournalAudit entree = JournalAudit.builder()
                .code("AUD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .dateAction(Instant.now())
                .action(action)
                .objet(objet)
                .motif(description)
                .utilisateur(auteur)
                .build();
        enregistrer(entree);
    }

    /**
     * Trace une action métier. L'acteur est l'e-mail capturé par le controller
     * avant l'appel : le SecurityContext n'est pas disponible dans ce thread @Async.
     * Un échec d'écriture est journalisé et n'est jamais propagé.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enregistrer(String action, String nomEntite, Long idEntite, String acteurEmail,
                            String valeurAvant, String valeurApres, String motif) {
        try {
            if (acteurEmail == null || acteurEmail.isBlank()) {
                log.error("Audit non enregistré [{}] : acteur absent", action);
                return;
            }
            Utilisateur auteur = utilisateurRepository.findByEmail(acteurEmail).orElse(null);
            if (auteur == null) {
                log.error("Audit non enregistré [{}] : utilisateur introuvable pour {}", action, acteurEmail);
                return;
            }
            JournalAudit entree = JournalAudit.builder()
                    .code("AUD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .dateAction(Instant.now())
                    .action(action)
                    .objet(idEntite == null ? nomEntite : nomEntite + "#" + idEntite)
                    .valeurAvant(commeJson(valeurAvant))
                    .valeurApres(commeJson(valeurApres))
                    .motif(motif)
                    .utilisateur(auteur)
                    .build();
            journalAuditRepository.save(entree);
        } catch (Exception ex) {
            log.error("Erreur lors de la persistance de l'audit [action={}] : {}", action, ex.getMessage());
        }
    }

    private String commeJson(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }
        String texte = valeur.trim();
        try {
            if ((texte.startsWith("{") && texte.endsWith("}"))
                    || (texte.startsWith("[") && texte.endsWith("]"))
                    || (texte.startsWith("\"") && texte.endsWith("\""))) {
                objectMapper.readTree(texte);
                return texte;
            }
        } catch (Exception ignored) {
            // La valeur n'est pas du JSON : elle sera encodée comme chaîne.
        }
        try {
            return objectMapper.writeValueAsString(valeur);
        } catch (Exception ex) {
            log.error("Valeur d'audit non sérialisable : {}", ex.getMessage());
            return "\"\"";
        }
    }

    @Transactional(readOnly = true)
    public Page<JournalAudit> listerParUtilisateur(String utilisateur, Pageable pageable) {
        return journalAuditRepository.findByUtilisateur_Email(utilisateur, pageable);
    }

    @Transactional(readOnly = true)
    public Page<JournalAudit> listerParEntite(String nomEntite, Long idEntite, Pageable pageable) {
        String objet = idEntite == null ? nomEntite : nomEntite + "#" + idEntite;
        return journalAuditRepository.findByObjet(objet, pageable);
    }
}
