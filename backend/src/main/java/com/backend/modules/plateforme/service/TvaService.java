package com.backend.modules.plateforme.service;

import com.backend.common.audit.JournalAuditService;
import com.backend.common.exception.BusinessRuleException;
import com.backend.common.tenant.TenantExecutor;
import com.backend.modules.plateforme.entity.Laboratoire;
import com.backend.modules.plateforme.repository.LaboratoireRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * TVA : les prix du catalogue sont saisis TTC. Le taux appliqué à un laboratoire est
 * son propre taux s'il en a défini un, sinon celui de la plateforme.
 * La part de TVA comprise dans un montant TTC vaut {@code TTC × taux / (100 + taux)}.
 */
@Service
@RequiredArgsConstructor
public class TvaService {

    private static final BigDecimal CENT = new BigDecimal("100");

    private final LaboratoireRepository laboratoireRepository;
    private final ParametrePlateformeService parametrePlateformeService;
    private final TenantExecutor tenantExecutor;
    private final JournalAuditService journalAuditService;

    /** Taux propre au laboratoire, taux de la plateforme et taux effectivement appliqué. */
    public record TvaLaboratoire(BigDecimal tauxLaboratoire, BigDecimal tauxPlateforme, BigDecimal tauxEffectif) {
    }

    public TvaLaboratoire pourLaboratoire(Long laboratoireId) {
        BigDecimal propre = tenantExecutor.inCentral(() -> laboratoireRepository.findById(laboratoireId)
                .orElseThrow(() -> new BusinessRuleException("LABORATOIRE_NON_TROUVE", "Laboratoire non trouvé"))
                .getTauxTva());
        BigDecimal plateforme = parametrePlateformeService.tauxTvaPlateforme();
        return new TvaLaboratoire(propre, plateforme, propre != null ? propre : plateforme);
    }

    /** Taux appliqué au laboratoire du schéma donné ; taux de la plateforme si le schéma est inconnu. */
    public BigDecimal tauxEffectifParSchema(String nomSchema) {
        BigDecimal propre = nomSchema == null ? null : tenantExecutor.inCentral(() ->
                laboratoireRepository.findByNomSchema(nomSchema).map(Laboratoire::getTauxTva).orElse(null));
        return propre != null ? propre : parametrePlateformeService.tauxTvaPlateforme();
    }

    /** Définit (ou retire, avec {@code null}) le taux propre au laboratoire. */
    public TvaLaboratoire modifierTauxLaboratoire(Long laboratoireId, BigDecimal taux, String acteurEmail) {
        if (taux != null && (taux.signum() < 0 || taux.compareTo(CENT) > 0)) {
            throw new BusinessRuleException("TAUX_TVA_INVALIDE", "Le taux de TVA doit être compris entre 0 et 100 %");
        }
        BigDecimal nouveau = taux == null ? null : taux.setScale(2, RoundingMode.HALF_UP);
        BigDecimal ancien = tenantExecutor.inCentral(() -> {
            Laboratoire laboratoire = laboratoireRepository.findById(laboratoireId)
                    .orElseThrow(() -> new BusinessRuleException("LABORATOIRE_NON_TROUVE", "Laboratoire non trouvé"));
            BigDecimal avant = laboratoire.getTauxTva();
            laboratoire.setTauxTva(nouveau);
            laboratoireRepository.save(laboratoire);
            return avant;
        });
        journalAuditService.enregistrer("MODIFIER_TVA_LABORATOIRE", "Laboratoire", laboratoireId, acteurEmail,
                "{\"tauxTva\":" + (ancien == null ? "null" : ancien.toPlainString()) + "}",
                "{\"tauxTva\":" + (nouveau == null ? "null" : nouveau.toPlainString()) + "}", null, laboratoireId);
        return pourLaboratoire(laboratoireId);
    }

    /** Part de TVA comprise dans un montant TTC, arrondie au centime. */
    public static BigDecimal tvaComprise(BigDecimal montantTtc, BigDecimal taux) {
        if (montantTtc == null || taux == null || taux.signum() == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return montantTtc.multiply(taux).divide(CENT.add(taux), 2, RoundingMode.HALF_UP);
    }
}
