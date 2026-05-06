package org.example.ilink.chat.persona.service;

import org.example.ilink.chat.persona.domain.PersonaChunk;
import org.example.ilink.chat.persona.domain.PersonaCorrections;
import org.example.ilink.chat.persona.domain.PersonaCore;
import org.example.ilink.chat.persona.domain.PersonaProfile;
import org.example.ilink.chat.persona.domain.RelationshipMemory;
import org.example.ilink.chat.persona.manager.ActivePersonaManager;
import org.example.ilink.chat.persona.repository.PersonaProfileRepository;
import org.example.ilink.chat.service.ChatStylePromptService;
import org.example.ilink.chat.service.WeChatPrivateChatExtractService;
import org.example.ilink.chat.session.service.ChatSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

@Service
public class PersonaRagService {

    private static final Logger log = LoggerFactory.getLogger(PersonaRagService.class);
    private static final int EMBEDDING_BATCH_SIZE = 4;

    @Autowired
    private WeChatPrivateChatExtractService weChatPrivateChatExtractService;

    @Autowired
    private PersonaChunkingService personaChunkingService;

    @Autowired
    private PersonaEmbeddingService personaEmbeddingService;

    @Autowired
    private QdrantPersonaStoreService qdrantPersonaStoreService;

    @Autowired
    private PersonaProfileRepository personaProfileRepository;

    @Autowired
    private ActivePersonaManager activePersonaManager;

    @Autowired
    private ChatStylePromptService chatStylePromptService;

    @Autowired
    private ChatSessionService chatSessionService;

    @Autowired
    @Qualifier("personaEmbeddingExecutor")
    private Executor personaEmbeddingExecutor;

    @Autowired
    @Qualifier("personaAnalysisExecutor")
    private Executor personaAnalysisExecutor;

    public PersonaProfile importFromPrivateDb(String personaName,
                                              String dbPath,
                                              String tableName,
                                              int limit,
                                              int incomingOriginSource,
                                              int outgoingOriginSource,
                                              String modelName) {
        String transcript = weChatPrivateChatExtractService.extractTranscript(
                dbPath,
                tableName,
                limit,
                incomingOriginSource,
                outgoingOriginSource
        );
        return importFromTranscript(personaName, transcript, tableName, modelName);
    }

    public PersonaProfile importFromTranscript(String personaName,
                                               String transcript,
                                               String sourceName,
                                               String modelName) {
        if (personaName == null || personaName.isBlank()) {
            throw new IllegalArgumentException("personaName 不能为空");
        }
        if (transcript == null || transcript.isBlank()) {
            throw new IllegalArgumentException("未能提取有效 transcript");
        }

        long startedAt = System.currentTimeMillis();
        int lineCount = countTranscriptLines(transcript);
        log.info("[PersonaImport] start persona={}, source={}, transcriptLines={}, model={}", personaName, sourceName, lineCount, modelName);

        List<String> chunks = personaChunkingService.chunkTranscript(transcript);
        if (chunks.isEmpty()) {
            throw new IllegalArgumentException("聊天记录不足，无法切分为可检索片段");
        }
        log.info("[PersonaImport] chunking done persona={}, chunks={}, embeddingBatchSize={}", personaName, chunks.size(), EMBEDDING_BATCH_SIZE);

        try {
            CompletableFuture<List<List<Float>>> embeddingsFuture = CompletableFuture.supplyAsync(
                    () -> embedChunks(personaName, chunks),
                    personaEmbeddingExecutor
            );
            CompletableFuture<String> personaCardFuture = CompletableFuture.supplyAsync(
                    () -> {
                        log.info("[PersonaImport] personaCard start persona={}", personaName);
                        String result = chatStylePromptService.generatePromptFromTranscript(personaName, transcript, modelName);
                        log.info("[PersonaImport] personaCard done persona={}", personaName);
                        return result;
                    },
                    personaAnalysisExecutor
            );
            CompletableFuture<PersonaCore> personaCoreFuture = CompletableFuture.supplyAsync(
                    () -> {
                        log.info("[PersonaImport] personaCore start persona={}", personaName);
                        PersonaCore result = chatStylePromptService.generatePersonaCore(personaName, transcript, modelName);
                        log.info("[PersonaImport] personaCore done persona={}", personaName);
                        return result;
                    },
                    personaAnalysisExecutor
            );
            CompletableFuture<RelationshipMemory> relationshipMemoryFuture = CompletableFuture.supplyAsync(
                    () -> {
                        log.info("[PersonaImport] relationshipMemory start persona={}", personaName);
                        RelationshipMemory result = chatStylePromptService.generateRelationshipMemory(personaName, transcript, modelName);
                        log.info("[PersonaImport] relationshipMemory done persona={}", personaName);
                        return result;
                    },
                    personaAnalysisExecutor
            );

            List<List<Float>> embeddings = embeddingsFuture.join();
            log.info("[PersonaImport] qdrant upsert start persona={}, chunks={}", personaName, chunks.size());
            qdrantPersonaStoreService.upsertChunks(personaName, chunks, embeddings);
            log.info("[PersonaImport] qdrant upsert done persona={}", personaName);

            String personaCard = personaCardFuture.join();
            if (personaCard == null || personaCard.isBlank()) {
                personaCard = "请模仿“" + personaName + "”与我聊天时的真实说话方式，优先学习其常见用词、接话习惯、情绪反应和口头禅。";
                log.info("[PersonaImport] personaCard fallback applied persona={}", personaName);
            }

            PersonaCore personaCore = personaCoreFuture.join();
            RelationshipMemory relationshipMemory = relationshipMemoryFuture.join();

            PersonaProfile profile = personaProfileRepository.save(
                    personaName,
                    personaCard,
                    sourceName,
                    personaCore,
                    relationshipMemory,
                    null
            );
            log.info("[PersonaImport] saved profile persona={}, durationMs={}", personaName, System.currentTimeMillis() - startedAt);
            return profile;
        } catch (CompletionException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            log.error("[PersonaImport] failed persona={}, durationMs={}, error={}", personaName, System.currentTimeMillis() - startedAt, cause.getMessage(), cause);
            throw new IllegalArgumentException("导入 persona 失败：" + cause.getMessage(), cause);
        }
    }

    public PersonaProfile activate(String personaName) {
        PersonaProfile profile = personaProfileRepository.findByName(personaName);
        if (profile == null) {
            throw new IllegalArgumentException("persona 不存在，请先导入：" + personaName);
        }
        activePersonaManager.setActivePersona(profile);
        return profile;
    }

    public PersonaProfile getActivePersona() {
        PersonaProfile activePersona = activePersonaManager.getActivePersona();
        if (activePersona != null) {
            return activePersona;
        }
        String activePersonaName = activePersonaManager.getActivePersonaName();
        if (activePersonaName == null || activePersonaName.isBlank()) {
            return null;
        }
        PersonaProfile restored = personaProfileRepository.findByName(activePersonaName);
        if (restored != null) {
            activePersonaManager.setActivePersona(restored);
        }
        return restored;
    }

    public void clearActivePersona() {
        activePersonaManager.clear();
    }

    public PersonaProfile getPersonaProfile(String personaName) {
        if (personaName == null || personaName.isBlank()) {
            throw new IllegalArgumentException("personaName 不能为空");
        }
        PersonaProfile profile = personaProfileRepository.findByName(personaName.trim());
        if (profile == null) {
            throw new IllegalArgumentException("persona 不存在：" + personaName);
        }
        return profile;
    }

    public PersonaProfile addCorrection(String personaName,
                                        String neverSay,
                                        String preferSay,
                                        String situationRule,
                                        String toneRule) {
        PersonaProfile profile = getPersonaProfile(personaName);
        PersonaCorrections corrections = profile.getCorrections();
        if (corrections == null) {
            corrections = new PersonaCorrections();
            profile.setCorrections(corrections);
        }
        appendIfPresent(corrections.getNeverSay(), neverSay);
        appendIfPresent(corrections.getPreferSay(), preferSay);
        appendIfPresent(corrections.getSituationRules(), situationRule);
        appendIfPresent(corrections.getToneRules(), toneRule);
        return personaProfileRepository.saveProfile(profile);
    }

    public List<PersonaChunk> retrieveRelevantChunks(String userMessage, int topK) {
        PersonaProfile activePersona = getActivePersona();
        if (activePersona == null || userMessage == null || userMessage.isBlank()) {
            return List.of();
        }
        List<Float> queryEmbedding = personaEmbeddingService.embed(userMessage);
        return qdrantPersonaStoreService.search(activePersona.getPersonaName(), queryEmbedding, topK);
    }

    public List<PersonaChunk> debugRetrieve(String personaName, String userMessage, int topK) {
        PersonaProfile profile = getPersonaProfile(personaName);
        if (userMessage == null || userMessage.isBlank()) {
            return List.of();
        }
        List<Float> queryEmbedding = personaEmbeddingService.embed(userMessage);
        return qdrantPersonaStoreService.search(profile.getPersonaName(), queryEmbedding, topK);
    }

    private List<List<Float>> embedChunks(String personaName, List<String> chunks) {
        List<List<Float>> embeddings = new ArrayList<>(chunks.size());
        int totalBatches = (chunks.size() + EMBEDDING_BATCH_SIZE - 1) / EMBEDDING_BATCH_SIZE;
        log.info("[PersonaImport] embeddings start persona={}, chunks={}, batches={}", personaName, chunks.size(), totalBatches);
        for (int start = 0; start < chunks.size(); start += EMBEDDING_BATCH_SIZE) {
            int end = Math.min(start + EMBEDDING_BATCH_SIZE, chunks.size());
            int batchNo = start / EMBEDDING_BATCH_SIZE + 1;
            long batchStartedAt = System.currentTimeMillis();
            log.info("[PersonaImport] embedding batch start persona={}, batch={}/{}, chunkRange={}..{}",
                    personaName, batchNo, totalBatches, start + 1, end);
            List<CompletableFuture<List<Float>>> futures = chunks.subList(start, end).stream()
                    .map(chunk -> CompletableFuture.supplyAsync(
                            () -> personaEmbeddingService.embed(chunk),
                            personaEmbeddingExecutor
                    ))
                    .toList();
            futures.stream()
                    .map(CompletableFuture::join)
                    .forEach(embeddings::add);
            log.info("[PersonaImport] embedding batch done persona={}, batch={}/{}, durationMs={}",
                    personaName, batchNo, totalBatches, System.currentTimeMillis() - batchStartedAt);
        }
        log.info("[PersonaImport] embeddings done persona={}, vectors={}", personaName, embeddings.size());
        return embeddings;
    }

    private int countTranscriptLines(String transcript) {
        if (transcript == null || transcript.isBlank()) {
            return 0;
        }
        int count = 0;
        String[] lines = transcript.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        for (String line : lines) {
            if (line != null && !line.isBlank()) {
                count++;
            }
        }
        return count;
    }

    private void appendIfPresent(List<String> target, String value) {
        if (target == null || value == null) {
            return;
        }
        String normalized = value.trim();
        if (!normalized.isBlank() && !target.contains(normalized)) {
            target.add(normalized);
        }
    }
}
