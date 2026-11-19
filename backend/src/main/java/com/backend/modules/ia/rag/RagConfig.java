package com.backend.modules.ia.rag;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Index de l'assistant IA.
 *
 * <p>Les embeddings sont calculés en local (modèle ONNX multilingue, voir
 * {@code spring.ai.embedding.transformer} dans application.yaml) : Groq ne fournit
 * pas de modèle d'embeddings, et rien ne sort du serveur pour l'indexation.
 * L'index reste en mémoire ; il est reconstruit au démarrage (quelques secondes).
 */
@Configuration
public class RagConfig {

    @Bean
    public SimpleVectorStore vectorStoreAssistant(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}
