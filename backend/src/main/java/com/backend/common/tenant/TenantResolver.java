package com.backend.common.tenant;

import com.backend.common.exception.UnauthorizedTenantException;
import com.backend.modules.plateforme.entity.Laboratoire;
import com.backend.modules.plateforme.repository.LaboratoireRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TenantResolver {

    public static final String STATUT_ACTIF = "ACTIF";

    private final LaboratoireRepository laboratoireRepository;
    private final TenantExecutor tenantExecutor;

    public Laboratoire resoudre(String codeOuSchema) {
        return tenantExecutor.inCentral(() -> laboratoireRepository.findByCode(codeOuSchema)
                .or(() -> laboratoireRepository.findByNomSchema(codeOuSchema))
                .orElseThrow(() -> new UnauthorizedTenantException(
                        "Laboratoire introuvable : " + codeOuSchema)));
    }

    public Laboratoire exigerActif(String codeOuSchema) {
        Laboratoire laboratoire = resoudre(codeOuSchema);
        if (!STATUT_ACTIF.equals(laboratoire.getStatut())) {
            throw new UnauthorizedTenantException(
                    "Le laboratoire '" + laboratoire.getCode() + "' n'est pas sélectionnable");
        }
        return laboratoire;
    }
}
