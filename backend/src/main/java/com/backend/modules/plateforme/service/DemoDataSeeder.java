package com.backend.modules.plateforme.service;

import com.backend.common.tenant.TenantProvisioner;
import com.backend.modules.plateforme.entity.Laboratoire;
import com.backend.modules.plateforme.repository.LaboratoireRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Après le seed Flyway central, crée les bases tenant manquantes et y applique
 * les migrations (y compris {@code V22__seed_demo_tenant.sql}).
 */
@Slf4j
@Component
@Order(100)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.demo-seed", havingValue = "true", matchIfMissing = true)
public class DemoDataSeeder implements ApplicationRunner {

    private static final String STATUT_ACTIF = "ACTIF";

    private final LaboratoireRepository laboratoireRepository;
    private final TenantProvisioner tenantProvisioner;

    @Override
    public void run(ApplicationArguments args) {
        for (Laboratoire laboratoire : laboratoireRepository.findByStatutOrderByRaisonSocialeAsc(STATUT_ACTIF)) {
            try {
                tenantProvisioner.provisionner(laboratoire);
            } catch (Exception ex) {
                log.warn("Provisionnement du laboratoire '{}' impossible : {}",
                        laboratoire.getCode(), ex.getMessage());
            }
        }
    }
}
