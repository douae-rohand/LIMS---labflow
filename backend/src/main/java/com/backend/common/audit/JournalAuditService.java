package com.backend.common.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service de journalisation d'audit.
 *
 * <p>Les méthodes d'enregistrement sont exécutées de façon asynchrone
 * ({@code @Async}) dans une transaction indépendante ({@code REQUIRES_NEW})
 * pour ne pas bloquer la transaction principale et garantir la persistance
 * même en cas de rollback du flux appelant.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JournalAuditService {

    private final JournalAuditRepository journalAuditRepository;

    // -------------------------------------------------------------------------
    // Enregistrement
    // -------------------------------------------------------------------------

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enregistrer(JournalAudit entree) {
        try {
            journalAuditRepository.save(entree);
        } catch (Exception ex) {
            // L'audit ne doit jamais faire échouer le flux principal
            log.error("Erreur lors de la persistance de l'audit [action={}] : {}",
                    entree.getAction(), ex.getMessage());
        }
    }

    /**
     * Raccourci pour les actions simples.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enregistrer(String action, String nomEntite, Long idEntite,
                            String utilisateur, String description) {
        JournalAudit entree = JournalAudit.builder()
                .action(action)
                .nomEntite(nomEntite)
                .idEntite(idEntite)
                .utilisateur(utilisateur)
                .description(description)
                .build();
        enregistrer(entree);
    }

    // -------------------------------------------------------------------------
    // Consultation
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<JournalAudit> listerParUtilisateur(String utilisateur, Pageable pageable) {
        return journalAuditRepository.findByUtilisateur(utilisateur, pageable);
    }

    @Transactional(readOnly = true)
    public Page<JournalAudit> listerParEntite(String nomEntite, Long idEntite, Pageable pageable) {
        return journalAuditRepository.findByNomEntiteAndIdEntite(nomEntite, idEntite, pageable);
    }
}
