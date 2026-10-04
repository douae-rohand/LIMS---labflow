package com.backend.common.audit;

import com.backend.modules.utilisateur.entity.Utilisateur;
import com.backend.modules.utilisateur.repository.UtilisateurRepository;
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
