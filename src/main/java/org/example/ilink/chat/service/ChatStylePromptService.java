package org.example.ilink.chat.service;

import org.example.ilink.chat.persona.domain.PersonaCore;
import org.example.ilink.chat.persona.domain.RelationshipMemory;

public interface ChatStylePromptService {

    /**
     * 根据当前会话最近聊天记录生成风格 prompt
     */
    String generatePromptFromRecentMessages(String contextToken, String modelName);

    /**
     * 根据指定说话人和聊天记录生成风格 prompt
     */
    String generatePromptFromTranscript(String targetSpeaker, String transcript, String modelName);

    PersonaCore generatePersonaCore(String personaName, String transcript, String modelName);

    RelationshipMemory generateRelationshipMemory(String personaName, String transcript, String modelName);
}
