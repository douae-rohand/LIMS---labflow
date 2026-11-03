package com.backend.common.audit;

import com.backend.modules.plateforme.entity.Laboratoire;
import com.backend.modules.plateforme.repository.LaboratoireRepository;
import com.backend.modules.utilisateur.entity.Utilisateur;
import com.backend.modules.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Écriture du journal dans une transaction neuve, sur le thread asynchrone.
 * L'acteur est déjà résolu par e-mail : le SecurityContext n'est pas propagé.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JournalAuditEcriture {

    private final JournalAuditRepository journalAuditRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final LaboratoireRepository laboratoireRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persister(String action, String objet, String valeurAvant, String valeurApres,
                          String motif, String acteurEmail, Instant dateAction, Long laboratoireId) {
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
            Laboratoire laboratoire = null;
            if (laboratoireId != null) {
                laboratoire = laboratoireRepository.findById(laboratoireId).orElse(null);
                if (laboratoire == null) {
                    log.warn("Audit [{}] enregistré sans laboratoire : id {} introuvable", action, laboratoireId);
                }
            }
            JournalAudit entree = JournalAudit.builder()
                    .code("AUD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .dateAction(dateAction == null ? Instant.now() : dateAction)
                    .action(action)
                    .objet(objet)
                    .valeurAvant(valeurAvant)
                    .valeurApres(valeurApres)
                    .motif(motif)
                    .utilisateur(auteur)
                    .laboratoire(laboratoire)
                    .build();
            journalAuditRepository.save(entree);
        } catch (Exception ex) {
            log.error("Erreur lors de la persistance de l'audit [action={}] : {}", action, ex.getMessage());
        }
    }
}
