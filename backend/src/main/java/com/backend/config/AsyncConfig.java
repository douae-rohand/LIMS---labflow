package com.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Exécuteur borné pour toutes les méthodes @Async de l'application.
 *
 * <p>L'exécuteur par défaut de Spring est un SimpleAsyncTaskExecutor (non borné).
 * Ce bean le remplace par un pool threadé dont la taille et la file d'attente
 * sont configurées pour éviter toute saturation en production.
 */
@Slf4j
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    @Bean(name = "taskExecutor")
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-email-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }

    /**
     * Gestionnaire d'exceptions pour les méthodes @Async à retour void.
     * N'enregistre jamais de contenu d'e-mail (adresse, lien, corps).
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) ->
            log.error("[async] Exception non capturée dans {}.{}() — message : {}",
                    method.getDeclaringClass().getSimpleName(),
                    method.getName(),
                    throwable.getMessage());
    }
}
