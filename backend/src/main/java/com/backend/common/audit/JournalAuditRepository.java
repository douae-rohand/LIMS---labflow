package com.backend.common.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface JournalAuditRepository extends JpaRepository<JournalAudit, Long> {

    Page<JournalAudit> findByUtilisateur_Email(String email, Pageable pageable);

    Page<JournalAudit> findByObjet(String objet, Pageable pageable);

    List<JournalAudit> findByDateActionBetween(Instant debut, Instant fin);

    Page<JournalAudit> findByAction(String action, Pageable pageable);
}
