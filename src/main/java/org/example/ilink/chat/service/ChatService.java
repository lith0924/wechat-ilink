package org.example.ilink.chat.service;

import org.example.ilink.chat.domain.entity.ChatMessage;
import org.example.ilink.chat.domain.entity.ChatReply;

import java.util.List;

public interface ChatService {

    /**
     * 保存用户消息，返回插入后的 id（用于关联回复）
     */
    Long saveMessage(String contextToken, String content, int useAi, String modelName, String chatMode);

    /**
     * 保存 Bot 回复
     */
    void saveReply(Long messageId, String content, int totalTokens);

    /**
     * 查询某个 contextToken 下的所有消息
     */
    List<ChatMessage> getMessagesByContextToken(String contextToken);

    /**
     * 查询某条消息的所有回复
     */
    List<ChatReply> getRepliesByMessageId(Long messageId);

    /**
     * 查询某个 contextToken 下最近的用户聊天消息
     */
    List<ChatMessage> getRecentAiMessagesByContextToken(String contextToken, int limit);
}
