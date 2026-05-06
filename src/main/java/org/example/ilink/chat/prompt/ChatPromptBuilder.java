package org.example.ilink.chat.prompt;

import org.example.ilink.chat.persona.domain.PersonaChunk;
import org.example.ilink.chat.persona.domain.PersonaCore;
import org.example.ilink.chat.persona.domain.PersonaCorrections;
import org.example.ilink.chat.persona.domain.PersonaProfile;
import org.example.ilink.chat.persona.domain.RelationshipMemory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ChatPromptBuilder {

    public String buildPrompt(String mode,
                              String customPrompt,
                              String styleTranscript,
                              String userMessage,
                              PersonaProfile activePersona,
                              List<PersonaChunk> relevantChunks) {
        String normalizedMode = mode == null || mode.isBlank() ? "default" : mode.toLowerCase();
        StringBuilder builder = new StringBuilder();

        switch (normalizedMode) {
            case "knowledge":
                builder.append("你现在是一个专业知识助手，请优先给出准确、结构化、简洁但有信息量的回答。如果信息不确定，请明确说明不确定。\n\n");
                break;
            case "cosplay":
                builder.append("你现在进入角色扮演模式，请保持自然、有个性、但不要脱离用户问题本身。\n\n");
                break;
            case "default":
            default:
                break;
        }

        if (activePersona != null) {
            builder.append("你现在要扮演“")
                    .append(activePersona.getPersonaName())
                    .append("”，继续和我聊天。你的目标不是总结风格，也不是做普通助手回复，而是尽量模仿这个人在真实聊天里对我说话的方式。\n\n")
                    .append("优先级规则：人工纠偏规则 > 真实检索片段 > 关系记忆 > 人格骨架 > 人物卡。\n")
                    .append("如果不同信息源之间出现冲突，以优先级更高的内容为准。\n\n")
                    .append("不要解释你在模仿。\n")
                    .append("不要总结规则。\n")
                    .append("不要复述历史片段。\n")
                    .append("不要回复得过于完整、过于标准、过于像 AI。\n")
                    .append("尽量像本人一样自然、口语化、贴近真实聊天；如果当前消息和历史片段中的某个场景高度相似，可以优先复用相似的回应结构，但不要机械照抄。\n\n");

            appendPersonaCore(builder, activePersona.getPersonaCore());
            appendRelationshipMemory(builder, activePersona.getRelationshipMemory());
            appendCorrections(builder, activePersona.getCorrections());

            if (activePersona.getPersonaCard() != null && !activePersona.getPersonaCard().isBlank()) {
                builder.append("人物卡（低优先级辅助参考）：\n")
                        .append(activePersona.getPersonaCard().trim())
                        .append("\n\n");
            }
            if (relevantChunks != null && !relevantChunks.isEmpty()) {
                builder.append("真实检索片段（高优先级参考）：\n");
                for (int i = 0; i < relevantChunks.size(); i++) {
                    builder.append("相关历史片段")
                            .append(i + 1)
                            .append("：\n")
                            .append(relevantChunks.get(i).getContent())
                            .append("\n\n");
                }
            }
            builder.append("当前消息：\n")
                    .append(userMessage);
            return builder.toString();
        }

        if (styleTranscript != null && !styleTranscript.isBlank()) {
            builder.append("下面是“我”和“对方”的真实历史聊天记录。你现在不要总结风格，也不要解释自己在模仿。\n")
                    .append("你的任务是直接学习“对方”过去对“我”的具体话术、口头禅、接话方式、常见用词、梗、情绪表达和回复行为。\n")
                    .append("接下来当“我”发送新消息时，你要像“对方本人”一样自然续聊，只回复“对方”会说的话，不要复述历史记录。\n\n")
                    .append("历史聊天记录：\n")
                    .append(styleTranscript.trim())
                    .append("\n\n")
                    .append("当前新消息（来自“我”）：")
                    .append(userMessage);
            return builder.toString();
        }

        if (customPrompt != null && !customPrompt.isBlank()) {
            builder.append("以下是当前会话的自定义风格要求，请严格遵守：\n")
                    .append(customPrompt.trim())
                    .append("\n\n");
        }

        builder.append("用户消息：")
                .append(userMessage);
        return builder.toString();
    }

    private void appendPersonaCore(StringBuilder builder, PersonaCore personaCore) {
        if (personaCore == null) {
            return;
        }
        builder.append("人格骨架：\n");
        appendNamedField(builder, "身份关系", personaCore.getIdentity());
        appendNamedField(builder, "语气基调", personaCore.getTone());
        appendNamedField(builder, "回复习惯", personaCore.getReplyStyle());
        appendNamedField(builder, "情绪模式", personaCore.getEmotionalPattern());
        appendNamedField(builder, "关心方式", personaCore.getCarePattern());
        appendNamedField(builder, "禁忌表达", personaCore.getTabooPatterns());
        appendNamedField(builder, "标志性表达", personaCore.getSignaturePhrases());
        builder.append("\n");
    }

    private void appendRelationshipMemory(StringBuilder builder, RelationshipMemory relationshipMemory) {
        if (relationshipMemory == null) {
            return;
        }
        builder.append("关系记忆：\n");
        appendListSection(builder, "共同经历", relationshipMemory.getSharedExperiences());
        appendListSection(builder, "内部梗", relationshipMemory.getInsideJokes());
        appendListSection(builder, "称呼", relationshipMemory.getNicknames());
        appendListSection(builder, "高频话题", relationshipMemory.getRecurringTopics());
        appendListSection(builder, "冲突模式", relationshipMemory.getConflictPatterns());
        appendListSection(builder, "重要时刻", relationshipMemory.getImportantMoments());
        builder.append("\n");
    }

    private void appendCorrections(StringBuilder builder, PersonaCorrections corrections) {
        if (corrections == null) {
            return;
        }
        builder.append("人工纠偏规则（最高优先级）：\n");
        appendListSection(builder, "不要这样说", corrections.getNeverSay());
        appendListSection(builder, "更偏好这样说", corrections.getPreferSay());
        appendListSection(builder, "场景规则", corrections.getSituationRules());
        appendListSection(builder, "语气规则", corrections.getToneRules());
        builder.append("\n");
    }

    private void appendNamedField(StringBuilder builder, String name, String value) {
        if (value != null && !value.isBlank()) {
            builder.append(name)
                    .append("：")
                    .append(value.trim())
                    .append("\n");
        }
    }

    private void appendListSection(StringBuilder builder, String title, List<String> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        builder.append(title).append("：\n");
        for (String item : items) {
            if (item != null && !item.isBlank()) {
                builder.append("- ")
                        .append(item.trim())
                        .append("\n");
            }
        }
    }
}
