package com.backend.common.tenant;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/**
 * Exécute un bloc dans une transaction neuve, sur le schéma central
 * ou sur un tenant donné, puis restaure le contexte précédent.
 */
@Component
@RequiredArgsConstructor
public class TenantExecutor {

    private final PlatformTransactionManager transactionManager;

    public <T> T inCentral(Supplier<T> action) {
        return executer(null, action);
    }

    public <T> T inTenant(String nomSchema, Supplier<T> action) {
        return executer(nomSchema, action);
    }

    private <T> T executer(String nomSchema, Supplier<T> action) {
        String precedent = TenantContext.getCurrentTenant();
        if (nomSchema == null || nomSchema.isBlank()) {
            TenantContext.clear();
        } else {
            TenantContext.setCurrentTenant(nomSchema);
        }
        try {
            TransactionTemplate template = new TransactionTemplate(transactionManager);
            template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            return template.execute(status -> action.get());
        } finally {
            if (precedent == null || precedent.isBlank()) {
                TenantContext.clear();
            } else {
                TenantContext.setCurrentTenant(precedent);
            }
        }
    }
}
