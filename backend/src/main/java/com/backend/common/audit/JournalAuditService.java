package com.backend.common.audit;

import com.backend.common.security.SecurityUtils;
import com.backend.common.tenant.TenantContext;
import com.backend.common.util.ApresCommit;
import com.backend.modules.auth.security.UtilisateurPrincipal;
import com.backend.modules.plateforme.entity.Laboratoire;
import com.backend.modules.plateforme.repository.LaboratoireRepository;
import com.backend.modules.utilisateur.entity.Utilisateur;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class JournalAuditService {

    private final JournalAuditRepository journalAuditRepository;
    private final ObjectMapper objectMapper;
    private final JournalAuditEcriture ecriture;
    private final LaboratoireRepository laboratoireRepository;

    /**
     * Trace une action réussie après le commit de la transaction métier.
     * Un rollback n'écrit donc pas une ligne « réussi ».
     */
    public void enregistrer(JournalAudit entree) {
        if (entree == null) {
            return;
        }
        String email = emailActeur(entree.getUtilisateur());
        Long laboratoireId = resoudreLaboratoireId(entree.getObjet(), entree.getUtilisateur());
        planifier(entree.getAction(), entree.getObjet(), entree.getValeurAvant(), entree.getValeurApres(),
                entree.getMotif(), email, entree.getDateAction(), laboratoireId);
    }

    /**
     * Résout le laboratoire_id pour l'audit :
     * (a) laboratoire de l'acteur (utilisateur.laboratoire) s'il existe
     * (b) sinon, si l'objet commence par <nomSchema>/, laboratoire retrouvé par nom_schema
     * (c) sinon NULL
     */
    private Long resoudreLaboratoireId(String objet, Utilisateur utilisateur) {
        if (utilisateur == null) {
            return null;
        }
        // (a) laboratoire de l'acteur
        if (utilisateur.getLaboratoire() != null && utilisateur.getLaboratoire().getId() != null) {
            return utilisateur.getLaboratoire().getId();
        }
        // (b) depuis l'objet si c'est un objet tenant
        if (objet != null && objet.startsWith("lims_")) {
            int slashIndex = objet.indexOf('/');
            if (slashIndex > 0) {
                String nomSchema = objet.substring(5, slashIndex); // "lims_" est le préfixe
                try {
                    Laboratoire labo = laboratoireRepository.findByNomSchema(nomSchema).orElse(null);
                    if (labo != null) {
                        return labo.getId();
                    }
                } catch (Exception ex) {
                    log.warn("Impossible de résoudre laboratoire depuis l'objet {} : {}", objet, ex.getMessage());
                }
            }
        }
        // (c) NULL
        return null;
    }

    public void enregistrer(String action, String nomEntite, Long idEntite,
                            String utilisateur, String description) {
        String objet = idEntite == null ? nomEntite : nomEntite + "#" + idEntite;
        planifier(action, objet, null, null, description, utilisateur, Instant.now(), null);
    }

    public void enregistrer(String action, String nomEntite, Long idEntite, String acteurEmail,
                            String valeurAvant, String valeurApres, String motif) {
        enregistrer(action, nomEntite, idEntite, acteurEmail, valeurAvant, valeurApres, motif, null);
    }

    public void enregistrer(String action, String nomEntite, Long idEntite, String acteurEmail,
                            String valeurAvant, String valeurApres, String motif, Long laboratoireId) {
        String objet = idEntite == null ? nomEntite : nomEntite + "#" + idEntite;
        planifier(action, objet, commeJson(valeurAvant), commeJson(valeurApres), motif,
                acteurEmail, Instant.now(), laboratoireId);
    }

    /**
     * Ligne d'échec qui doit survivre au rollback (déprovisionnement orphelin).
     * Écrite tout de suite, dans une transaction neuve.
     */
    public void enregistrerEchec(String action, String nomEntite, Long idEntite, String acteurEmail,
                                 String valeurAvant, String valeurApres, String motif) {
        String objet = idEntite == null ? nomEntite : nomEntite + "#" + idEntite;
        ecriture.persister(action, objet, commeJson(valeurAvant), commeJson(valeurApres), motif,
                acteurEmail, Instant.now(), null);
    }

    private void planifier(String action, String objet, String valeurAvant, String valeurApres,
                           String motif, String acteurEmail, Instant dateAction, Long laboratoireId) {
        Instant quand = dateAction == null ? Instant.now() : dateAction;
        ApresCommit.executer(() -> ecriture.persister(
                action, objet, valeurAvant, valeurApres, motif, acteurEmail, quand, laboratoireId));
    }

    private static String emailActeur(Utilisateur utilisateur) {
        if (utilisateur == null) {
            return null;
        }
        return utilisateur.getEmail();
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
