package com.backend.modules.demande.service;

import com.backend.common.audit.JournalAuditService;
import com.backend.modules.demande.entity.Demande;
import com.backend.modules.demande.entity.StatutDemande;
import com.backend.modules.demande.repository.DemandeRepository;
import com.backend.modules.essai.entity.LigneEssai;
import com.backend.modules.essai.entity.StatutEssai;
import com.backend.modules.essai.repository.LigneEssaiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Avancement d'une demande acceptée tant qu'aucune analyse n'a démarré :
 * <ul>
 *   <li>ACCEPTEE : au moins une analyse réalisable sans échantillon conforme (attendu, partiel ou à remplacer) ;</li>
 *   <li>ECHANTILLONS_RECUS : toutes les analyses réalisables ont un échantillon conforme ;</li>
 *   <li>PLANIFIEE : en plus, toutes sont affectées à un technicien.</li>
 * </ul>
 * Le passage en EN_COURS se fait au premier « Démarrer » du technicien : ce service ne touche donc jamais
 * une demande déjà démarrée, refusée, annulée ou terminée.
 */
@Service
@RequiredArgsConstructor
public class AvancementDemandeService {

    private final LigneEssaiRepository ligneEssaiRepository;
    private final DemandeRepository demandeRepository;
    private final JournalAuditService journalAuditService;

    /**
     * Recalcule le statut d'avant démarrage et l'enregistre s'il change.
     *
     * @return le statut obtenu (inchangé si la demande n'est pas dans une phase d'avant démarrage)
     */
    public StatutDemande recalculer(Demande demande, String acteur, Long laboratoireId) {
        StatutDemande actuel = StatutDemande.depuis(demande.getStatut());
        if (!StatutDemande.AVANT_DEMARRAGE.contains(actuel)) {
            return actuel;
        }
        StatutDemande calcule = calculer(ligneEssaiRepository.findByDemande_Id(demande.getId()));
        if (calcule != actuel) {
            demande.setStatut(calcule.name());
            demandeRepository.save(demande);
            journalAuditService.enregistrer("CHANGEMENT_STATUT_DEMANDE", "Demande", demande.getId(), acteur,
                    "{\"statut\":\"" + actuel.name() + "\"}", "{\"statut\":\"" + calcule.name() + "\"}",
                    motif(calcule), laboratoireId);
        }
        return calcule;
    }

    /** Statut d'avant démarrage déduit des analyses de la demande. */
    static StatutDemande calculer(List<LigneEssai> lignes) {
        List<LigneEssai> realisables = lignes.stream()
                .filter(l -> l.getStatut() != StatutEssai.NON_REALISABLE)
                .toList();
        if (realisables.isEmpty()) {
            return StatutDemande.ACCEPTEE;
        }
        boolean toutesRecues = realisables.stream().allMatch(l -> l.getEchantillon() != null
                && Boolean.TRUE.equals(l.getEchantillon().getConformite()));
        if (!toutesRecues) {
            return StatutDemande.ACCEPTEE;
        }
        boolean toutesAffectees = realisables.stream().allMatch(l -> l.getTechnicienId() != null);
        return toutesAffectees ? StatutDemande.PLANIFIEE : StatutDemande.ECHANTILLONS_RECUS;
    }

    private static String motif(StatutDemande statut) {
        return switch (statut) {
            case ECHANTILLONS_RECUS -> "Tous les échantillons conformes sont reçus";
            case PLANIFIEE -> "Toutes les analyses sont affectées à un technicien";
            default -> "Échantillons encore attendus";
        };
    }
}
