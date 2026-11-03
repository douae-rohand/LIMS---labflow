package com.backend.common.audit.service;

import com.backend.common.audit.JournalAudit;
import com.backend.common.audit.JournalAuditRepository;
import com.backend.common.audit.dto.AuditEntryDto;
import com.backend.common.security.SecurityUtils;
import com.backend.modules.plateforme.entity.Laboratoire;
import com.backend.modules.plateforme.repository.LaboratoireRepository;
import com.backend.modules.utilisateur.entity.Utilisateur;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditAdminService {

    private final JournalAuditRepository journalAuditRepository;
    private final LaboratoireRepository laboratoireRepository;

    private static final List<String> ACTIONS_AUTORISEES = List.of(
            "CREATION_ESSAI", "MODIFICATION_ESSAI", "DESACTIVATION_ESSAI", "SUPPRESSION_ESSAI",
            "CREATION_DOMAINE", "MODIFICATION_DOMAINE", "SUPPRESSION_DOMAINE",
            "EMISSION_FACTURE", "ENCAISSEMENT_FACTURE",
            "CREATION_PRODUIT", "MODIFICATION_PRODUIT", "SUPPRESSION_PRODUIT",
            "ENTREE_LOT", "AJUSTEMENT_STOCK",
            "MODIFICATION_LABORATOIRE"
    );

    @Transactional(readOnly = true)
    public Page<AuditEntryDto> lister(String action, String typeObjet, String acteur,
                                          java.time.Instant debut, java.time.Instant fin,
                                          String q, org.springframework.data.domain.Pageable pageable) {
        Long laboratoireId = SecurityUtils.principalCourant().getLaboratoireId();
        if (laboratoireId == null) {
            throw new RuntimeException("Administrateur sans laboratoire");
        }

        Laboratoire laboratoire = laboratoireRepository.findById(laboratoireId)
                .orElseThrow(() -> new RuntimeException("Laboratoire non trouvé"));
        String nomSchema = laboratoire.getNomSchema();
        String laboratoireIdStr = laboratoireId.toString();

        Page<JournalAudit> page = journalAuditRepository.rechercher(
                acteur, action, typeObjet, nomSchema, laboratoireIdStr, debut, fin, q, pageable);

        return page.map(this::toDto);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<AuditEntryDto> toutesEntries(org.springframework.data.domain.Pageable pageable) {
        Long laboratoireId = SecurityUtils.principalCourant().getLaboratoireId();
        if (laboratoireId == null) {
            throw new RuntimeException("Administrateur sans laboratoire");
        }

        Laboratoire laboratoire = laboratoireRepository.findById(laboratoireId)
                .orElseThrow(() -> new RuntimeException("Laboratoire non trouvé"));
        String nomSchema = laboratoire.getNomSchema();
        String laboratoireIdStr = laboratoireId.toString();

        Page<JournalAudit> page = journalAuditRepository.rechercher(
                null, null, null, nomSchema, laboratoireIdStr, null, null, null, pageable);

        return page.map(this::toDto);
    }

    private AuditEntryDto toDto(JournalAudit audit) {
        String objetAffiche = audit.getObjet();
        if (objetAffiche != null && objetAffiche.startsWith("lims_")) {
            int slashIndex = objetAffiche.indexOf('/');
            if (slashIndex > 0) {
                objetAffiche = objetAffiche.substring(slashIndex + 1);
            }
        }

        Utilisateur utilisateur = audit.getUtilisateur();
        String acteurNom = utilisateur != null ? utilisateur.getNomComplet() : null;
        String acteurRole = utilisateur != null && utilisateur.getRole() != null ? utilisateur.getRole().getCode() : null;

        return AuditEntryDto.builder()
                .code(audit.getCode())
                .dateAction(audit.getDateAction())
                .action(audit.getAction())
                .objet(objetAffiche)
                .valeurAvant(audit.getValeurAvant())
                .valeurApres(audit.getValeurApres())
                .motif(audit.getMotif())
                .acteurNom(acteurNom)
                .acteurRole(acteurRole)
                .build();
    }
}
