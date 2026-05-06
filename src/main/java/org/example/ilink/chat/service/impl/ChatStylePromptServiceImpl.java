package org.example.ilink.chat.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.ilink.chat.domain.entity.ChatMessage;
import org.example.ilink.chat.persona.domain.PersonaCore;
import org.example.ilink.chat.persona.domain.RelationshipMemory;
import org.example.ilink.chat.service.ChatService;
import org.example.ilink.chat.service.ChatStylePromptService;
import org.example.ilink.config.AIConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatStylePromptServiceImpl implements ChatStylePromptService {

    private static final int DEFAULT_SAMPLE_SIZE = 12;
    private static final int MIN_SAMPLE_SIZE = 5;

    @Autowired
    private ChatService chatService;

    @Autowired
    private AIConfig aiConfig;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String generatePromptFromRecentMessages(String contextToken, String modelName) {
        List<ChatMessage> messages = chatService.getRecentAiMessagesByContextToken(contextToken, DEFAULT_SAMPLE_SIZE);
        if (messages.size() < MIN_SAMPLE_SIZE) {
            return null;
        }

        String samples = messages.stream()
                .map(ChatMessage::getContent)
                .filter(content -> content != null && !content.isBlank())
                .map(String::trim)
                .collect(Collectors.joining("\n"));

        if (samples.isBlank()) {
            return null;
        }

        String analysisPrompt = buildRecentMessagesAnalysisPrompt(samples);
        String result = aiConfig.generateResponse(modelName, analysisPrompt);
        return sanitizePrompt(result);
    }

    @Override
    public String generatePromptFromTranscript(String targetSpeaker, String transcript, String modelName) {
        if (targetSpeaker == null || targetSpeaker.isBlank() || transcript == null || transcript.isBlank()) {
            return null;
        }
        String analysisPrompt = buildTranscriptAnalysisPrompt(targetSpeaker.trim(), transcript.trim());
        String result = aiConfig.generateResponse(modelName, analysisPrompt);
        return sanitizePrompt(result);
    }

    @Override
    public PersonaCore generatePersonaCore(String personaName, String transcript, String modelName) {
        if (personaName == null || personaName.isBlank() || transcript == null || transcript.isBlank()) {
            return buildFallbackPersonaCore(personaName);
        }
        try {
            String analysisPrompt = buildPersonaCoreAnalysisPrompt(personaName.trim(), transcript.trim());
            String result = aiConfig.generateResponse(modelName, analysisPrompt);
            PersonaCore parsed = parsePersonaCore(result);
            return isEmptyPersonaCore(parsed) ? buildFallbackPersonaCore(personaName) : parsed;
        } catch (Exception e) {
            return buildFallbackPersonaCore(personaName);
        }
    }

    @Override
    public RelationshipMemory generateRelationshipMemory(String personaName, String transcript, String modelName) {
        if (transcript == null || transcript.isBlank()) {
            return new RelationshipMemory();
        }
        try {
            String analysisPrompt = buildRelationshipMemoryAnalysisPrompt(personaName == null ? "对方" : personaName.trim(), transcript.trim());
            String result = aiConfig.generateResponse(modelName, analysisPrompt);
            RelationshipMemory parsed = parseRelationshipMemory(result);
            return parsed == null ? new RelationshipMemory() : parsed;
        } catch (Exception e) {
            return new RelationshipMemory();
        }
    }

    private String buildRecentMessagesAnalysisPrompt(String samples) {
        return "请根据以下用户聊天内容，为一个替代式聊天助手生成可直接使用的角色提示词。\n"
                + "目标不是复述原话，而是学习此人平时与我聊天时的回复习惯。\n"
                + "要求：\n"
                + "1. 总结对方常见语气、口语程度、句长、节奏和常用表达。\n"
                + "2. 重点总结情绪强度、是否主动追问、是否会接话、安慰、调侃、敷衍或延展话题。\n"
                + "3. 输出要像给助手的系统指令，明确说明今后应如何像这个人一样回复我。\n"
                + "4. 不要复述具体聊天内容，不要泄露隐私，不要总结事件本身。\n"
                + "5. 输出控制在100到180字，不要加标题，不要加编号。\n\n"
                + "聊天样本：\n"
                + samples;
    }

    private String buildTranscriptAnalysisPrompt(String targetSpeaker, String transcript) {
        return "请阅读以下双人或多人聊天记录，只分析说话人“" + targetSpeaker + "”在与我对话时的回复习惯，并输出为一段后续对话可直接使用的角色提示词。\n"
                + "目标是让助手成为“" + targetSpeaker + "”的替代品，与我继续聊天。\n"
                + "要求：\n"
                + "1. 只关注“" + targetSpeaker + "”的回复方式，不分析其他说话人。\n"
                + "2. 重点提炼：语气风格、口语程度、句长、节奏、情绪强度、常见表达、是否主动追问、是否会顺着话题继续聊、是否爱安慰/调侃/敷衍。\n"
                + "3. 总结的是“这个人会怎么回我”，不是单纯总结文本风格。\n"
                + "4. 输出必须是给助手的指令语气，例如“请使用……方式回复，并在合适时……”。\n"
                + "5. 不要复述原句，不要泄露隐私，不要总结聊天事件本身。\n"
                + "6. 输出控制在100到180字，不要加标题，不要加编号。\n"
                + "7. 如果聊天记录不足以稳定判断该说话人的回复习惯，就输出“无法生成稳定风格提示词”。\n\n"
                + "聊天记录：\n"
                + transcript;
    }

    private String buildPersonaCoreAnalysisPrompt(String personaName, String transcript) {
        return "请根据以下聊天记录，分析“" + personaName + "”在与我聊天时的稳定人格与互动特征，并严格输出 JSON。\n"
                + "只分析“" + personaName + "”这个人，不要分析我。\n"
                + "输出字段必须且只能包含：identity,tone,replyStyle,emotionalPattern,carePattern,tabooPatterns,signaturePhrases。\n"
                + "每个字段输出 1 到 2 句简洁中文，不要使用数组，不要额外解释，不要输出 markdown。\n\n"
                + "聊天记录：\n"
                + transcript;
    }

    private String buildRelationshipMemoryAnalysisPrompt(String personaName, String transcript) {
        return "请根据以下聊天记录，提取我和“" + personaName + "”之间适合长期对话模仿使用的关系记忆，并严格输出 JSON。\n"
                + "输出字段必须且只能包含：sharedExperiences,insideJokes,nicknames,recurringTopics,conflictPatterns,importantMoments。\n"
                + "每个字段都输出字符串数组，每个数组最多 5 条，每条尽量简短具体。\n"
                + "不要编造不存在的信息；如果某类信息不足，就输出空数组。不要输出 markdown。\n\n"
                + "聊天记录：\n"
                + transcript;
    }

    private PersonaCore parsePersonaCore(String result) {
        try {
            JsonNode node = readJsonNode(result);
            PersonaCore core = new PersonaCore();
            core.setIdentity(readText(node, "identity"));
            core.setTone(readText(node, "tone"));
            core.setReplyStyle(readText(node, "replyStyle"));
            core.setEmotionalPattern(readText(node, "emotionalPattern"));
            core.setCarePattern(readText(node, "carePattern"));
            core.setTabooPatterns(readText(node, "tabooPatterns"));
            core.setSignaturePhrases(readText(node, "signaturePhrases"));
            return core;
        } catch (Exception e) {
            return null;
        }
    }

    private RelationshipMemory parseRelationshipMemory(String result) {
        try {
            JsonNode node = readJsonNode(result);
            RelationshipMemory memory = new RelationshipMemory();
            memory.setSharedExperiences(readTextList(node, "sharedExperiences"));
            memory.setInsideJokes(readTextList(node, "insideJokes"));
            memory.setNicknames(readTextList(node, "nicknames"));
            memory.setRecurringTopics(readTextList(node, "recurringTopics"));
            memory.setConflictPatterns(readTextList(node, "conflictPatterns"));
            memory.setImportantMoments(readTextList(node, "importantMoments"));
            return memory;
        } catch (Exception e) {
            return new RelationshipMemory();
        }
    }

    private JsonNode readJsonNode(String result) throws Exception {
        String cleaned = result == null ? "" : result.trim();
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start >= 0 && end >= start) {
            cleaned = cleaned.substring(start, end + 1);
        }
        return objectMapper.readTree(cleaned);
    }

    private String readText(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        if (field == null || field.isNull()) {
            return "";
        }
        return field.asText("").trim();
    }

    private List<String> readTextList(JsonNode node, String fieldName) {
        List<String> values = new ArrayList<>();
        JsonNode field = node.get(fieldName);
        if (field == null || !field.isArray()) {
            return values;
        }
        field.forEach(item -> {
            if (item != null) {
                String text = item.asText("").trim();
                if (!text.isBlank()) {
                    values.add(text);
                }
            }
        });
        return values;
    }

    private boolean isEmptyPersonaCore(PersonaCore core) {
        return core == null
                || allBlank(core.getIdentity(), core.getTone(), core.getReplyStyle(), core.getEmotionalPattern(), core.getCarePattern(), core.getTabooPatterns(), core.getSignaturePhrases());
    }

    private boolean allBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private PersonaCore buildFallbackPersonaCore(String personaName) {
        PersonaCore core = new PersonaCore();
        core.setIdentity(personaName == null || personaName.isBlank() ? "对方本人" : personaName + "，是一个需要被模仿真实聊天习惯的人。"
        );
        core.setTone("整体语气以自然口语化为主，不要过度书面，不要像标准客服或心理咨询模板。");
        core.setReplyStyle("优先短句和顺手接话，必要时追问一句，不要动不动长篇分析。");
        core.setEmotionalPattern("当我表达烦、累、无语或委屈时，优先按真实聊天里的习惯回应，再决定是否继续安慰或追问。");
        core.setCarePattern("关心方式应贴近真实片段里的表达，不要突然变得很肉麻或很官方。");
        core.setTabooPatterns("避免明显不属于本人风格的鸡汤、说教、客服腔和过度完整的标准答案。");
        core.setSignaturePhrases("优先从真实检索片段中学习他常用的短句和口头表达。");
        return core;
    }

    private String sanitizePrompt(String result) {
        if (result == null) {
            return null;
        }
        String cleaned = result.trim();
        cleaned = cleaned.replaceFirst("^(风格提示词|提示词|风格总结|角色提示词|角色设定)[:：]\\s*", "");
        if (cleaned.contains("无法生成稳定风格提示词")) {
            return null;
        }
        return cleaned.isBlank() ? null : cleaned;
    }
}
