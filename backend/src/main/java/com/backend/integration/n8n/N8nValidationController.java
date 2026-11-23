package com.backend.integration.n8n;

import com.backend.common.tenant.TenantExecutor;
import com.backend.modules.planification.dto.LigneTravailDto;
import com.backend.modules.plateforme.entity.Laboratoire;
import com.backend.modules.plateforme.repository.LaboratoireRepository;
import com.backend.modules.utilisateur.entity.RoleUtilisateur;
import com.backend.modules.utilisateur.entity.Utilisateur;
import com.backend.modules.utilisateur.repository.UtilisateurRepository;
import com.backend.modules.validation.service.ValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * W6 rappel-validation : résultats validés par le technicien qui attendent la décision du Responsable.
 * Protégé par le secret partagé ({@link N8nSecretFilter}) comme tout {@code /api/n8n/**}.
 * Aucune donnée d'identité (patient, client) n'est renvoyée : référence de demande et analyse seulement.
 */
@Slf4j
@RestController
@RequestMapping("/api/n8n")
@RequiredArgsConstructor
public class N8nValidationController {

    private static final int HEURES_MAX = 720;

    private final TenantExecutor tenantExecutor;
    private final LaboratoireRepository laboratoireRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ValidationService validationService;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    /**
     * Une ligne par couple (Responsable, résultat en attente).
     * <ul>
     *   <li>{@code heures = 0} : tous les résultats en attente (test manuel) ;</li>
     *   <li>{@code heures = X > 0} : seulement les résultats qui franchissent X h, 2X h, 3X h… d'attente pendant
     *       l'heure en cours. Avec un passage n8n toutes les heures, chaque résultat est donc rappelé une fois
     *       par tranche de X heures, et non à chaque passage.</li>
     * </ul>
     */
    @GetMapping("/resultats-en-attente")
    public Map<String, Object> resultatsEnAttente(@RequestParam(defaultValue = "24") int heures) {
        int seuil = Math.max(0, Math.min(heures, HEURES_MAX));
        Instant maintenant = Instant.now();
        List<Map<String, Object>> lignes = new ArrayList<>();
        List<Laboratoire> laboratoires = tenantExecutor.inCentral(
                () -> laboratoireRepository.findByStatutOrderByRaisonSocialeAsc("ACTIF"));
        for (Laboratoire laboratoire : laboratoires) {
            try {
                List<LigneTravailDto> enAttente = tenantExecutor.inTenant(laboratoire.getNomSchema(),
                        validationService::resultatsAValider).stream()
                        .filter(r -> aRappeler(r.getDerniereDecisionDate(), maintenant, seuil))
                        .sorted(Comparator.comparing((LigneTravailDto r) -> r.isCritique() ? 0 : 1)
                                .thenComparing(r -> r.getScoreAnomalie() == null ? 0 : -r.getScoreAnomalie())
                                .thenComparing(LigneTravailDto::getDerniereDecisionDate,
                                        Comparator.nullsLast(Comparator.naturalOrder())))
                        .toList();
                if (enAttente.isEmpty()) {
                    continue;
                }
                List<Utilisateur> responsables = tenantExecutor.inCentral(() -> utilisateurRepository
                        .findActifsParRoleEtLaboratoire(RoleUtilisateur.RESPONSABLE.name(), laboratoire.getId())
                        .stream().filter(u -> StringUtils.hasText(u.getEmail())).toList());
                for (Utilisateur responsable : responsables) {
                    for (LigneTravailDto resultat : enAttente) {
                        lignes.add(ligne(laboratoire, responsable, resultat, maintenant));
                    }
                }
            } catch (Exception ex) {
                log.warn("Lecture n8n des résultats en attente ignorée pour laboratoire {} : {}",
                        laboratoire.getId(), ex.getClass().getSimpleName());
            }
        }
        Map<String, Object> reponse = new LinkedHashMap<>();
        reponse.put("heures", seuil);
        reponse.put("total", lignes.size());
        reponse.put("resultats", lignes);
        return reponse;
    }

    static boolean aRappeler(Instant valideLe, Instant maintenant, int seuil) {
        if (valideLe == null) {
            return false;
        }
        long attente = Duration.between(valideLe, maintenant).toHours();
        if (seuil == 0) {
            return true;
        }
        return attente >= seuil && attente % seuil == 0;
    }

    private Map<String, Object> ligne(Laboratoire laboratoire, Utilisateur responsable, LigneTravailDto r,
                                      Instant maintenant) {
        Map<String, Object> ligne = new LinkedHashMap<>();
        ligne.put("responsableEmail", responsable.getEmail());
        ligne.put("responsableNom", responsable.getNomComplet());
        ligne.put("laboratoireId", laboratoire.getId());
        ligne.put("nomLaboratoire", laboratoire.getRaisonSociale());
        ligne.put("ligneEssaiId", r.getId());
        ligne.put("demandeReference", r.getDemandeReference());
        ligne.put("analyse", r.getEssaiLibelle());
        ligne.put("heuresAttente", Duration.between(r.getDerniereDecisionDate(), maintenant).toHours());
        ligne.put("critique", r.isCritique());
        ligne.put("scoreAnomalie", r.getScoreAnomalie());
        ligne.put("lienValidation", frontendUrl.replaceAll("/$", "") + "/app/validation-resultats");
        return ligne;
    }
}
