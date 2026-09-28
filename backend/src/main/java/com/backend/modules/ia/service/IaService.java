package com.backend.modules.ia.service;

import com.backend.modules.ia.dto.AnalyseIaRequest;
import com.backend.modules.ia.dto.AnalyseIaResponse;
import com.backend.integration.llm.LlmClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Service d'intégration IA (M14).
 * Délègue les appels au LLM via {@link LlmClientService}.
 *
 * TODO: stocker les demandes/réponses IA en base pour traçabilité et audit.
 * TODO: implémenter le RAG (Retrieval-Augmented Generation) sur la base documentaire.
 * TODO: gérer les limites de rate et timeouts du LLM.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IaService {

    private final LlmClientService llmClientService;

    /**
     * Soumet une tâche d'analyse IA au modèle LLM configuré.
     */
    public AnalyseIaResponse analyser(AnalyseIaRequest request) {
        log.info("Analyse IA demandée : type={}", request.getTypeTache());

        // TODO: construire un prompt structuré selon le type de tâche
        String prompt = construirePrompt(request);
        String resultat = llmClientService.appelerLlm(prompt);

        return AnalyseIaResponse.builder()
                .typeTache(request.getTypeTache())
                .resultat(resultat)
                .modeleUtilise(llmClientService.getModeleActif())
                .dateAnalyse(Instant.now())
                .build();
    }

    private String construirePrompt(AnalyseIaRequest request) {
        // TODO: templates de prompts par type de tâche (fichiers .txt ou base de données)
        return String.format("[%s] %s", request.getTypeTache().name(), request.getContexte());
    }
}
