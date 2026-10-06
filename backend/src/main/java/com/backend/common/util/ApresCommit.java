package com.backend.common.util;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Exécute une action après le commit de la transaction courante.
 * Évite d'envoyer un e-mail si le workflow métier est ensuite annulé.
 */
public final class ApresCommit {

    private ApresCommit() {
    }

    public static void executer(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
            return;
        }
        action.run();
    }
}
