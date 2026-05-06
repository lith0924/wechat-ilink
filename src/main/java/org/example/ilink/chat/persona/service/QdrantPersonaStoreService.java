package org.example.ilink.chat.persona.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.ilink.chat.persona.domain.PersonaChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class QdrantPersonaStoreService {

    private static final Logger log = LoggerFactory.getLogger(QdrantPersonaStoreService.class);
    private static final int UPSERT_BATCH_SIZE = 100;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${qdrant.url:http://localhost:6333}")
    private String qdrantUrl;

    @Value("${qdrant.collection:persona_chunks}")
    private String collectionName;

    @Value("${qdrant.api-key:}")
    private String qdrantApiKey;

    private WebClient buildClient() {
        WebClient.Builder builder = WebClient.builder().baseUrl(qdrantUrl);
        if (qdrantApiKey != null && !qdrantApiKey.isBlank()) {
            builder.defaultHeader("api-key", qdrantApiKey);
        }
        return builder.build();
    }

    public void upsertChunks(String personaName, List<String> chunks, List<List<Float>> embeddings) {
        int vectorSize = embeddings.isEmpty() ? 1024 : embeddings.get(0).size();
        ensureCollection(vectorSize);
        int totalBatches = (chunks.size() + UPSERT_BATCH_SIZE - 1) / UPSERT_BATCH_SIZE;
        log.info("[QdrantPersona] upsert start collection={}, persona={}, points={}, vectorSize={}, batches={}",
                collectionName, personaName, chunks.size(), vectorSize, totalBatches);

        for (int start = 0; start < chunks.size(); start += UPSERT_BATCH_SIZE) {
            int end = Math.min(start + UPSERT_BATCH_SIZE, chunks.size());
            int batchNo = start / UPSERT_BATCH_SIZE + 1;
            long startedAt = System.currentTimeMillis();
            List<Map<String, Object>> points = new ArrayList<>();
            for (int i = start; i < end; i++) {
                Map<String, Object> point = new HashMap<>();
                point.put("id", UUID.randomUUID().toString());
                point.put("vector", embeddings.get(i));

                Map<String, Object> payload = new HashMap<>();
                payload.put("personaName", personaName);
                payload.put("chunkIndex", i);
                payload.put("content", chunks.get(i));
                point.put("payload", payload);
                points.add(point);
            }

            Map<String, Object> body = new HashMap<>();
            body.put("points", points);

            try {
                log.info("[QdrantPersona] upsert batch start collection={}, persona={}, batch={}/{}, pointRange={}..{}",
                        collectionName, personaName, batchNo, totalBatches, start + 1, end);
                buildClient().put()
                        .uri("/collections/" + collectionName + "/points?wait=true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(body)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();
                log.info("[QdrantPersona] upsert batch done collection={}, persona={}, batch={}/{}, durationMs={}",
                        collectionName, personaName, batchNo, totalBatches, System.currentTimeMillis() - startedAt);
            } catch (WebClientResponseException e) {
                log.error("[QdrantPersona] upsert batch failed collection={}, persona={}, batch={}/{}, status={}, responseBody={}",
                        collectionName, personaName, batchNo, totalBatches, e.getStatusCode(), e.getResponseBodyAsString(), e);
                throw e;
            }
        }
        log.info("[QdrantPersona] upsert done collection={}, persona={}, points={}", collectionName, personaName, chunks.size());
    }

    public List<PersonaChunk> search(String personaName, List<Float> embedding, int topK) {
        Map<String, Object> filter = new HashMap<>();
        Map<String, Object> match = new HashMap<>();
        match.put("value", personaName);
        Map<String, Object> fieldCondition = new HashMap<>();
        fieldCondition.put("key", "personaName");
        fieldCondition.put("match", match);
        filter.put("must", List.of(fieldCondition));

        Map<String, Object> body = new HashMap<>();
        body.put("vector", embedding);
        body.put("limit", topK);
        body.put("with_payload", true);
        body.put("filter", filter);

        String response = buildClient().post()
                .uri("/collections/" + collectionName + "/points/search")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        try {
            Map<String, Object> parsed = objectMapper.readValue(response, new TypeReference<>() {});
            List<Map<String, Object>> result = (List<Map<String, Object>>) parsed.get("result");
            List<PersonaChunk> chunks = new ArrayList<>();
            if (result == null) {
                return chunks;
            }
            for (Map<String, Object> row : result) {
                Map<String, Object> payload = (Map<String, Object>) row.get("payload");
                chunks.add(new PersonaChunk(
                        String.valueOf(row.get("id")),
                        String.valueOf(payload.get("personaName")),
                        (Integer) payload.get("chunkIndex"),
                        String.valueOf(payload.get("content")),
                        row.get("score") == null ? null : Double.valueOf(String.valueOf(row.get("score")))
                ));
            }
            return chunks;
        } catch (Exception e) {
            throw new IllegalStateException("解析 Qdrant 检索结果失败", e);
        }
    }

    private void ensureCollection(int vectorSize) {
        try {
            buildClient().get()
                    .uri("/collections/" + collectionName)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            Map<String, Object> vectors = new HashMap<>();
            vectors.put("size", vectorSize);
            vectors.put("distance", "Cosine");
            Map<String, Object> body = new HashMap<>();
            body.put("vectors", vectors);
            buildClient().put()
                    .uri("/collections/" + collectionName)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        }
    }
}
