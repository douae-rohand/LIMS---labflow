package com.backend.modules.planification.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

@Service
public class EtatPaiementService {

    public static final String AUCUNE_FACTURE = "AUCUNE_FACTURE";
    public static final String NON_PAYEE = "NON_PAYEE";
    public static final String PARTIELLE = "PARTIELLE";
    public static final String PAYEE = "PAYEE";

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public Map<Long, String> etatsPaiement(Collection<Long> demandeIds) {
        Map<Long, String> etats = new HashMap<>();
        if (demandeIds.isEmpty()) {
            return etats;
        }
        var lignes = entityManager.createQuery("""
                SELECT f.demande.id, f.montantHt, f.statut,
                       (SELECT COALESCE(SUM(p.montant), 0) FROM Paiement p WHERE p.facture = f)
                FROM Facture f WHERE f.demande.id IN :ids
                """, Object[].class)
                .setParameter("ids", new HashSet<>(demandeIds))
                .getResultList();
        for (Object[] ligne : lignes) {
            BigDecimal total = (BigDecimal) ligne[1];
            BigDecimal paye = ligne[3] instanceof BigDecimal montant ? montant : BigDecimal.ZERO;
            String etat = PAYEE.equals(ligne[2]) || (total != null && paye.compareTo(total) >= 0
                    && paye.signum() > 0) ? PAYEE : paye.signum() > 0 ? PARTIELLE : NON_PAYEE;
            etats.put((Long) ligne[0], etat);
        }
        return etats;
    }
}
