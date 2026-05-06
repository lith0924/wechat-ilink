package org.example.ilink.chat.service.impl;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.example.ilink.chat.prompt.ChatPromptBuilder;
import org.example.ilink.chat.persona.domain.PersonaChunk;
import org.example.ilink.chat.persona.domain.PersonaProfile;
import org.example.ilink.chat.persona.service.PersonaRagService;
import org.example.ilink.chat.service.ChatAppService;
import org.example.ilink.chat.service.ChatService;
import org.example.ilink.chat.session.domain.ChatSessionState;
import org.example.ilink.chat.session.service.ChatSessionService;
import org.example.ilink.config.AIConfig;
import org.example.ilink.strategy.AIResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ChatAppServiceImpl implements ChatAppService {

    private static final Pattern NAME_PATTERN = Pattern.compile("(?:我叫|我是|我的名字是)([\\u4e00-\\u9fa5A-Za-z0-9·]{1,20})");
    private static final Pattern ASK_NAME_PATTERN = Pattern.compile("(记得|知道).*(我叫|我的名字)|我叫什么|我的名字是什么|我是谁");

    @Autowired
    private ChatSessionService chatSessionService;

    @Autowired
    private ChatPromptBuilder chatPromptBuilder;

    @Autowired
    private PersonaRagService personaRagService;

    @Autowired
    private AIConfig aiConfig;

    @Autowired
    private ChatService chatService;

    @Value("${chat.memory.max-messages:20}")
    private int maxMessages;

    private final Map<String, MessageWindowChatMemory> memoryStore = new ConcurrentHashMap<>();
    private final Map<String, SessionFacts> sessionFactsStore = new ConcurrentHashMap<>();

    @Override
    public String handleChat(String conversationId, String userMessage) {
        ChatSessionState sessionState = chatSessionService.getSession(conversationId);
        String mode = sessionState.getMode();
        String modelName = sessionState.getModel();
        String customPrompt = sessionState.getCustomPrompt();
        String styleTranscript = sessionState.getStyleTranscript();
        PersonaProfile activePersona = personaRagService.getActivePersona();
        List<PersonaChunk> relevantChunks = personaRagService.retrieveRelevantChunks(userMessage, 6);
        String prompt = chatPromptBuilder.buildPrompt(mode, customPrompt, styleTranscript, userMessage, activePersona, relevantChunks);

        SessionFacts sessionFacts = sessionFactsStore.computeIfAbsent(conversationId, key -> new SessionFacts());
        boolean askingUserName = isAskingUserName(userMessage);
        if (!askingUserName) {
            updateSessionFacts(sessionFacts, userMessage);
        }

        if (askingUserName && sessionFacts.userName() != null && !sessionFacts.userName().isBlank()) {
            String directAnswer = "你叫" + sessionFacts.userName() + "。";
            Long messageId = chatService.saveMessage(conversationId, userMessage, 1, modelName, mode);
            AIResponse aiResult = new AIResponse(directAnswer, userMessage.length() / 2 + 1, directAnswer.length() / 2 + 1);
            chatService.saveReply(messageId, aiResult.getContent(), aiResult.getTotalTokens());
            remember(conversationId, buildSystemPrompt(sessionFacts, styleTranscript), userMessage, directAnswer);
            return directAnswer;
        }

        Long messageId = chatService.saveMessage(conversationId, userMessage, 1, modelName, mode);
        String aiResponse = generateWithMemory(conversationId, modelName, sessionFacts, styleTranscript, prompt);
        AIResponse aiResult = new AIResponse(aiResponse, userMessage.length() / 2 + 1, aiResponse.length() / 2 + 1);
        chatService.saveReply(messageId, aiResult.getContent(), aiResult.getTotalTokens());
        return aiResponse;
    }

    public void clearMemory(String conversationId) {
        memoryStore.remove(conversationId);
        sessionFactsStore.remove(conversationId);
    }

    private String generateWithMemory(String conversationId, String modelName, SessionFacts sessionFacts, String styleTranscript, String prompt) {
        MessageWindowChatMemory memory = memoryStore.computeIfAbsent(
                conversationId,
                key -> MessageWindowChatMemory.withMaxMessages(Math.max(2, maxMessages))
        );

        List<ChatMessage> messages = new ArrayList<>(memory.messages());
        if (messages.isEmpty()) {
            messages.add(new SystemMessage(buildSystemPrompt(sessionFacts, styleTranscript)));
        } else if (messages.get(0) instanceof SystemMessage) {
            messages.set(0, new SystemMessage(buildSystemPrompt(sessionFacts, styleTranscript)));
        } else {
            messages.add(0, new SystemMessage(buildSystemPrompt(sessionFacts, styleTranscript)));
        }

        messages.add(new UserMessage(prompt));
        ChatLanguageModel model = aiConfig.getChatLanguageModel(modelName);
        String aiResponse = model.generate(messages).content().text();

        remember(conversationId, buildSystemPrompt(sessionFacts, styleTranscript), prompt, aiResponse);
        return aiResponse;
    }

    private void remember(String conversationId, String systemPrompt, String userMessage, String aiResponse) {
        MessageWindowChatMemory memory = memoryStore.computeIfAbsent(
                conversationId,
                key -> MessageWindowChatMemory.withMaxMessages(Math.max(2, maxMessages))
        );

        List<ChatMessage> existing = memory.messages();
        if (existing.isEmpty()) {
            memory.add(new SystemMessage(systemPrompt));
        } else if (existing.get(0) instanceof SystemMessage) {
            existing.set(0, new SystemMessage(systemPrompt));
        }

        memory.add(new UserMessage(userMessage));
        memory.add(new AiMessage(aiResponse));
    }

    private String buildSystemPrompt(SessionFacts sessionFacts, String styleTranscript) {
        StringBuilder builder = new StringBuilder("你是一个持续对话助手。必须优先依据已知会话信息回答，不要编造用户身份。");
        PersonaProfile activePersona = personaRagService.getActivePersona();
        if (activePersona != null) {
            builder.append(" 当前会话已启用 persona 检索模仿模式。你要按以下优先级生成回复：人工纠偏规则 > 真实检索片段 > 关系记忆 > 人格骨架 > 人物卡。")
                    .append("优先依据检索到的真实聊天片段，模仿“")
                    .append(activePersona.getPersonaName())
                    .append("”与我聊天时的具体话术行为、口头禅、接话方式和情绪反应；如果不同信息源冲突，以高优先级内容为准。");
        }
        if (styleTranscript != null && !styleTranscript.isBlank()) {
            builder.append(" 当前会话已启用聊天记录直喂模仿模式，你要优先模仿历史样本中的“对方”与“我”聊天时的话术行为，而不是进行抽象风格总结。");
        }
        if (sessionFacts.userName() != null && !sessionFacts.userName().isBlank()) {
            builder.append(" 用户名字是“")
                    .append(sessionFacts.userName())
                    .append("”。当用户问你记不记得他叫什么时，应直接回答这个名字。");
        }
        return builder.toString();
    }

    private void updateSessionFacts(SessionFacts sessionFacts, String userMessage) {
        Matcher matcher = NAME_PATTERN.matcher(userMessage);
        if (matcher.find()) {
            String extractedName = matcher.group(1);
            if (extractedName != null) {
                extractedName = extractedName.replaceAll("[？?吗呢呀啊吧啦]+$", "").trim();
            }
            if (extractedName != null && !extractedName.isBlank()) {
                sessionFacts.setUserName(extractedName);
            }
        }
    }

    private boolean isAskingUserName(String userMessage) {
        return ASK_NAME_PATTERN.matcher(userMessage).find();
    }

    private static class SessionFacts {
        private String userName;

        public String userName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }
    }
}
