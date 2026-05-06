package org.example.ilink.chat.persona.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PersonaEmbeddingService {

    @Value("${ai.embedding.api-key:}")
    private String embeddingApiKey;

    @Value("${ai.embedding.api-url}")
    private String embeddingApiUrl;

    @Value("${ai.embedding.model:text-embedding-v3}")
    private String embeddingModelName;

    public List<Float> embed(String text) {
        EmbeddingModel model = OpenAiEmbeddingModel.builder()
                .apiKey(embeddingApiKey)
                .baseUrl(embeddingApiUrl)
                .modelName(embeddingModelName)
                .build();
        Embedding embedding = model.embed(text).content();
        return embedding.vectorAsList();
    }
}
