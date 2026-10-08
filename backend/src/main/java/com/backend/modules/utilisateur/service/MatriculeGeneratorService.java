package com.backend.modules.utilisateur.service;

import com.backend.modules.utilisateur.entity.MatriculeSequence;
import com.backend.modules.utilisateur.entity.RoleUtilisateur;
import com.backend.modules.utilisateur.repository.MatriculeSequenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;

/**
 * Service de génération atomique et séquentielle des matricules utilisateurs.
 *
 * <p>Format : {@code PREFIXE-ANNEE-000000} (ex: {@code TEC-2026-000017})
 *
 * <p>Sécurité concurrence : utilise un verrou pessimiste en écriture ({@code SELECT FOR UPDATE})
 * pour garantir l'unicité stricte du numéro séquentiel sous forte charge réseau.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatriculeGeneratorService {

    private final MatriculeSequenceRepository sequenceRepository;

    /**
     * Génère un matricule unique et permanent pour un rôle donné.
     *
     * @param roleCode Code du rôle (ex: "TECHNICIEN", "CLIENT", "ADMINISTRATEUR", etc.)
     * @return Matricule généré au format {@code PREFIXE-ANNEE-NUMERO_6_CHIFFRES}
     */
    @Transactional
    public String genererMatricule(String roleCode) {
        String prefixe = obtenirPrefixe(roleCode);
        int annee = Year.now().getValue();

        MatriculeSequence seq = sequenceRepository.findWithLock(prefixe, annee)
                .orElseGet(() -> {
                    MatriculeSequence nouvelleSeq = MatriculeSequence.builder()
                            .prefixe(prefixe)
                            .annee(annee)
                            .dernierNumero(0)
                            .build();
                    return sequenceRepository.saveAndFlush(nouvelleSeq);
                });

        int prochainNumero = seq.getDernierNumero() + 1;
        seq.setDernierNumero(prochainNumero);
        sequenceRepository.save(seq);

        String matricule = String.format("%s-%d-%06d", prefixe, annee, prochainNumero);
        log.debug("Matricule généré : {} (role={})", matricule, roleCode);
        return matricule;
    }

    /**
     * Surcharge acceptant l'enum {@link RoleUtilisateur}.
     */
    @Transactional
    public String genererMatricule(RoleUtilisateur role) {
        return genererMatricule(role != null ? role.name() : null);
    }

    private String obtenirPrefixe(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return "USR";
        }
        return switch (roleCode.toUpperCase().trim()) {
            case "CLIENT" -> "CLI";
            case "ADMINISTRATEUR" -> "ADM";
            case "RESPONSABLE", "RESPONSABLE_LABO" -> "RES";
            case "TECHNICIEN" -> "TEC";
            case "ACCUEIL" -> "ACC";
            case "SUPER_ADMINISTRATEUR" -> "SAD";
            default -> "USR";
        };
    }
}
