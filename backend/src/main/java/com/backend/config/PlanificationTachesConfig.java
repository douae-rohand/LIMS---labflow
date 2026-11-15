package com.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Active les tâches planifiées ({@code @Scheduled}) : refus automatique des demandes patients
 * sans échantillon sous 48 h, nettoyage des limiteurs de débit d'authentification.
 */
@Configuration
@EnableScheduling
public class PlanificationTachesConfig {
}
