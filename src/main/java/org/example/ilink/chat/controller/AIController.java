package org.example.ilink.chat.controller;

import org.example.ilink.chat.persona.domain.PersonaChunk;
import org.example.ilink.chat.persona.domain.PersonaProfile;
import org.example.ilink.chat.persona.service.PersonaRagService;
import org.example.ilink.chat.persona.service.PersonaTranscriptTextParser;
import org.example.ilink.chat.service.ChatService;
import org.example.ilink.chat.service.ChatStylePromptService;
import org.example.ilink.chat.service.WeChatPrivateChatExtractService;
import org.example.ilink.chat.service.impl.ChatAppServiceImpl;
import org.example.ilink.chat.session.domain.ChatSessionState;
import org.example.ilink.chat.session.service.ChatSessionService;
import org.example.ilink.config.AIConfig;
import org.example.ilink.message.manager.MessageManager;
import org.example.ilink.strategy.AIResponse;
import org.example.ilink.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@CrossOrigin
@RestController
@RequestMapping("/api/ai")
public class AIController {

    @Autowired
    private AIConfig aiConfig;

    @Autowired
    private ChatService chatService;

    @Autowired
    private ChatStylePromptService chatStylePromptService;

    @Autowired
    private ChatSessionService chatSessionService;

    @Autowired
    private ChatAppServiceImpl chatAppService;

    @Autowired
    private WeChatPrivateChatExtractService weChatPrivateChatExtractService;

    @Autowired
    private PersonaRagService personaRagService;

    @Autowired
    private PersonaTranscriptTextParser personaTranscriptTextParser;

    @Autowired
    private MessageManager messageManager;

    /**
     * 使用默认模型生成回复
     */
    @PostMapping("/generate")
    public Result generateResponse(
            @RequestParam String prompt,
            @RequestParam(required = false) String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }
        AIResponse aiResult = aiConfig.generateWithUsage(prompt);
        Long messageId = chatService.saveMessage(sessionId, prompt, 1, aiConfig.getDefaultModelName(), "default");
        chatService.saveReply(messageId, aiResult.getContent(), aiResult.getTotalTokens());
        Map<String, Object> data = new HashMap<>();
        data.put("response", aiResult.getContent());
        data.put("sessionId", sessionId);
        data.put("totalTokens", aiResult.getTotalTokens());
        return Result.success(data);
    }

    /**
     * 使用指定模型生成回复
     */
    @PostMapping("/generate/{modelName}")
    public Result generateResponseWithModel(
            @PathVariable String modelName,
            @RequestParam String prompt,
            @RequestParam(required = false) String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }
        String response = aiConfig.generateResponse(modelName, prompt);
        Long messageId = chatService.saveMessage(sessionId, prompt, 1, modelName, "default");
        int totalTokens = (prompt.length() + response.length()) / 2 + 1;
        chatService.saveReply(messageId, response, totalTokens);
        Map<String, Object> data = new HashMap<>();
        data.put("model", modelName);
        data.put("response", response);
        data.put("sessionId", sessionId);
        return Result.success(data);
    }

    /**
     * 上传聊天记录文件并生成风格 prompt
     */
    @PostMapping(value = "/prompt/from-chat-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result generatePromptFromChatFile(
            @RequestParam String targetSpeaker,
            @RequestParam MultipartFile file,
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) String modelName) {
        if (targetSpeaker == null || targetSpeaker.isBlank()) {
            return Result.error("targetSpeaker 不能为空");
        }
        if (file == null || file.isEmpty()) {
            return Result.error("聊天记录文件不能为空");
        }
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }

        String transcript;
        try {
            transcript = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return Result.error("读取聊天记录文件失败：" + e.getMessage());
        }

        if (transcript.isBlank()) {
            return Result.error("聊天记录文件内容为空");
        }

        String resolvedModel = resolveModelName(sessionId, modelName);
        String generatedPrompt = chatStylePromptService.generatePromptFromTranscript(targetSpeaker, transcript, resolvedModel);
        if (generatedPrompt == null || generatedPrompt.isBlank()) {
            return Result.error("未能从该聊天记录中生成稳定的风格 prompt，请补充更多目标说话人的聊天内容后再试");
        }

        ChatSessionState state = chatSessionService.updateCustomPrompt(sessionId, generatedPrompt);
        chatSessionService.clearStyleTranscript(sessionId);
        chatAppService.clearMemory(sessionId);

        Map<String, Object> data = new HashMap<>();
        data.put("sessionId", sessionId);
        data.put("targetSpeaker", targetSpeaker);
        data.put("model", resolvedModel);
        data.put("prompt", state.getCustomPrompt());
        data.put("fileName", file.getOriginalFilename());
        return Result.success(data);
    }

    /**
     * 列出解密微信数据库中的消息表
     */
    @GetMapping("/wechat-db/tables")
    public Result listWeChatDbTables(@RequestParam String dbPath) {
        try {
            List<String> tables = weChatPrivateChatExtractService.listMessageTables(dbPath);
            Map<String, Object> data = new HashMap<>();
            data.put("tables", tables);
            data.put("count", tables.size());
            return Result.success(data);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 预览指定消息表最近消息
     */
    @GetMapping("/wechat-db/preview")
    public Result previewWeChatDbMessages(
            @RequestParam String dbPath,
            @RequestParam String tableName,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            List<String> messages = weChatPrivateChatExtractService.previewRawMessages(dbPath, tableName, limit);
            Map<String, Object> data = new HashMap<>();
            data.put("tableName", tableName);
            data.put("messages", messages);
            return Result.success(data);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 从私聊数据库表提取 transcript 并生成 prompt
     */
    @PostMapping("/prompt/from-private-db")
    public Result generatePromptFromPrivateDb(
            @RequestParam String dbPath,
            @RequestParam String tableName,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "2") int incomingOriginSource,
            @RequestParam(defaultValue = "1") int outgoingOriginSource,
            @RequestParam(defaultValue = "对方") String targetSpeaker,
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) String modelName) {
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }
        try {
            String transcript = weChatPrivateChatExtractService.extractTranscript(
                    dbPath,
                    tableName,
                    limit,
                    incomingOriginSource,
                    outgoingOriginSource
            );
            if (transcript == null || transcript.isBlank()) {
                return Result.error("未能从数据库中提取有效 transcript，请先检查表名或 origin_source 映射");
            }
            String resolvedModel = resolveModelName(sessionId, modelName);
            String generatedPrompt = chatStylePromptService.generatePromptFromTranscript(targetSpeaker, transcript, resolvedModel);
            if (generatedPrompt == null || generatedPrompt.isBlank()) {
                return Result.error("已提取 transcript，但未能生成稳定风格 prompt，请增加更多消息样本或调整目标说话人");
            }

            ChatSessionState state = chatSessionService.updateCustomPrompt(sessionId, generatedPrompt);
            chatSessionService.clearStyleTranscript(sessionId);
            chatAppService.clearMemory(sessionId);

            Map<String, Object> data = new HashMap<>();
            data.put("sessionId", sessionId);
            data.put("model", resolvedModel);
            data.put("tableName", tableName);
            data.put("targetSpeaker", targetSpeaker);
            data.put("transcript", transcript);
            data.put("prompt", state.getCustomPrompt());
            return Result.success(data);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 从私聊数据库表提取 transcript 并直接写入当前会话，后续聊天直接模仿“对方”话术行为
     */
    @PostMapping("/imitate/from-private-db")
    public Result imitateFromPrivateDb(
            @RequestParam String dbPath,
            @RequestParam String tableName,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "2") int incomingOriginSource,
            @RequestParam(defaultValue = "1") int outgoingOriginSource,
            @RequestParam(required = false) String sessionId) {
        String resolvedSessionId = resolveConversationSessionId(sessionId);
        if (resolvedSessionId == null || resolvedSessionId.isBlank()) {
            return Result.error("当前没有可用的活跃聊天会话，请先在微信里和 bot 发生一条真实对话，或显式传入 sessionId");
        }
        try {
            String transcript = weChatPrivateChatExtractService.extractTranscript(
                    dbPath,
                    tableName,
                    limit,
                    incomingOriginSource,
                    outgoingOriginSource
            );
            if (transcript == null || transcript.isBlank()) {
                return Result.error("未能从数据库中提取有效 transcript，请先检查表名或 origin_source 映射");
            }

            ChatSessionState state = chatSessionService.updateStyleTranscript(resolvedSessionId, transcript);
            chatSessionService.clearCustomPrompt(resolvedSessionId);
            chatAppService.clearMemory(resolvedSessionId);

            Map<String, Object> data = new HashMap<>();
            data.put("sessionId", resolvedSessionId);
            data.put("tableName", tableName);
            data.put("mode", "direct-transcript-imitation");
            data.put("transcript", state.getStyleTranscript());
            return Result.success(data);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取所有可用的模型
     */
    @GetMapping("/models")
    public Result getAvailableModels() {
        Map<String, Object> data = new HashMap<>();
        data.put("models", aiConfig.getAvailableModels());
        return Result.success(data);
    }

    @PostMapping("/persona/import-from-private-db")
    public Result importPersonaFromPrivateDb(
            @RequestParam String personaName,
            @RequestParam String dbPath,
            @RequestParam String tableName,
            @RequestParam(defaultValue = "80") int limit,
            @RequestParam(defaultValue = "2") int incomingOriginSource,
            @RequestParam(defaultValue = "1") int outgoingOriginSource,
            @RequestParam(required = false) String modelName) {
        if (personaName == null || personaName.isBlank()) {
            return Result.error("personaName 不能为空");
        }
        try {
            String resolvedModel = resolveModelName(resolveConversationSessionId(null), modelName);
            PersonaProfile profile = personaRagService.importFromPrivateDb(
                    personaName.trim(),
                    dbPath,
                    tableName,
                    limit,
                    incomingOriginSource,
                    outgoingOriginSource,
                    resolvedModel
            );
            return Result.success(toPersonaData(profile));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/persona/import-from-transcript")
    public Result importPersonaFromTranscript(
            @RequestParam String personaName,
            @RequestParam String transcript,
            @RequestParam(required = false, defaultValue = "我") String selfSpeaker,
            @RequestParam(required = false) String personaSpeaker,
            @RequestParam(required = false) String sourceName,
            @RequestParam(required = false) String modelName) {
        if (personaName == null || personaName.isBlank()) {
            return Result.error("personaName 不能为空");
        }
        try {
            String resolvedPersonaSpeaker = personaSpeaker == null || personaSpeaker.isBlank() ? personaName.trim() : personaSpeaker.trim();
            String parsedTranscript = personaTranscriptTextParser.parse(transcript, selfSpeaker, resolvedPersonaSpeaker);
            String resolvedModel = resolveModelName(resolveConversationSessionId(null), modelName);
            PersonaProfile profile = personaRagService.importFromTranscript(
                    personaName.trim(),
                    parsedTranscript,
                    sourceName == null || sourceName.isBlank() ? "txt-transcript" : sourceName.trim(),
                    resolvedModel
            );
            Map<String, Object> data = toPersonaData(profile);
            data.put("transcript", parsedTranscript);
            return Result.success(data);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping(value = "/persona/import-from-transcript-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result importPersonaFromTranscriptFile(
            @RequestParam String personaName,
            @RequestParam MultipartFile file,
            @RequestParam(required = false, defaultValue = "我") String selfSpeaker,
            @RequestParam(required = false) String personaSpeaker,
            @RequestParam(required = false) String sourceName,
            @RequestParam(required = false) String modelName) {
        if (personaName == null || personaName.isBlank()) {
            return Result.error("personaName 不能为空");
        }
        if (file == null || file.isEmpty()) {
            return Result.error("聊天记录 txt 文件不能为空");
        }
        try {
            String rawText = new String(file.getBytes(), StandardCharsets.UTF_8);
            String resolvedPersonaSpeaker = personaSpeaker == null || personaSpeaker.isBlank() ? personaName.trim() : personaSpeaker.trim();
            String parsedTranscript = personaTranscriptTextParser.parse(rawText, selfSpeaker, resolvedPersonaSpeaker);
            String resolvedModel = resolveModelName(resolveConversationSessionId(null), modelName);
            String resolvedSourceName = sourceName == null || sourceName.isBlank()
                    ? file.getOriginalFilename()
                    : sourceName.trim();
            PersonaProfile profile = personaRagService.importFromTranscript(
                    personaName.trim(),
                    parsedTranscript,
                    resolvedSourceName == null || resolvedSourceName.isBlank() ? "txt-transcript-file" : resolvedSourceName,
                    resolvedModel
            );
            Map<String, Object> data = toPersonaData(profile);
            data.put("fileName", file.getOriginalFilename());
            data.put("transcript", parsedTranscript);
            return Result.success(data);
        } catch (IOException e) {
            return Result.error("读取聊天记录 txt 文件失败：" + e.getMessage());
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/persona/activate")
    public Result activatePersona(@RequestParam String personaName) {
        if (personaName == null || personaName.isBlank()) {
            return Result.error("personaName 不能为空");
        }
        try {
            PersonaProfile profile = personaRagService.activate(personaName.trim());
            return Result.success(toPersonaData(profile));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/persona/clear")
    public Result clearActivePersona() {
        personaRagService.clearActivePersona();
        return Result.success();
    }

    @GetMapping("/persona/current")
    public Result currentPersona() {
        PersonaProfile profile = personaRagService.getActivePersona();
        Map<String, Object> data = profile == null ? new HashMap<>() : toPersonaData(profile);
        return Result.success(data);
    }

    @GetMapping("/persona/detail")
    public Result getPersonaDetail(@RequestParam String personaName) {
        try {
            return Result.success(toPersonaData(personaRagService.getPersonaProfile(personaName)));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/persona/correction")
    public Result addPersonaCorrection(
            @RequestParam String personaName,
            @RequestParam(required = false) String neverSay,
            @RequestParam(required = false) String preferSay,
            @RequestParam(required = false) String situationRule,
            @RequestParam(required = false) String toneRule) {
        try {
            PersonaProfile profile = personaRagService.addCorrection(personaName, neverSay, preferSay, situationRule, toneRule);
            return Result.success(toPersonaData(profile));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/persona/retrieval-preview")
    public Result previewPersonaRetrieval(
            @RequestParam String personaName,
            @RequestParam String userMessage,
            @RequestParam(defaultValue = "6") int topK) {
        try {
            List<PersonaChunk> chunks = personaRagService.debugRetrieve(personaName, userMessage, topK);
            Map<String, Object> data = new HashMap<>();
            data.put("personaName", personaName);
            data.put("userMessage", userMessage);
            data.put("topK", topK);
            data.put("chunks", chunks);
            return Result.success(data);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/session/current")
    public Result getCurrentConversationSession() {
        Map<String, Object> data = new HashMap<>();
        data.put("sessionId", messageManager.getLastConversationKey());
        data.put("contextToken", messageManager.getContextToken());
        data.put("clientId", messageManager.getClientId());
        return Result.success(data);
    }

    private Map<String, Object> toPersonaData(PersonaProfile profile) {
        Map<String, Object> data = new HashMap<>();
        data.put("personaName", profile.getPersonaName());
        data.put("personaCard", profile.getPersonaCard());
        data.put("sourceTable", profile.getSourceTable());
        data.put("personaCore", profile.getPersonaCore());
        data.put("relationshipMemory", profile.getRelationshipMemory());
        data.put("corrections", profile.getCorrections());
        data.put("updatedAt", profile.getUpdatedAt());
        return data;
    }

    private String resolveConversationSessionId(String sessionId) {
        if (sessionId != null && !sessionId.isBlank()) {
            return sessionId;
        }
        String lastConversationKey = messageManager.getLastConversationKey();
        if (lastConversationKey != null && !lastConversationKey.isBlank()) {
            return lastConversationKey;
        }
        String contextToken = messageManager.getContextToken();
        if (contextToken != null && !contextToken.isBlank()) {
            return contextToken;
        }
        return null;
    }

    private String resolveModelName(String sessionId, String modelName) {
        if (modelName != null && !modelName.isBlank()) {
            return modelName;
        }
        ChatSessionState state = chatSessionService.getSession(sessionId);
        if (state != null && state.getModel() != null && !state.getModel().isBlank()) {
            return state.getModel();
        }
        return aiConfig.getDefaultModelName();
    }
}
