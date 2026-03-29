package org.example.ilink.service;

import org.example.ilink.entity.chat.ChatRecord;
import org.example.ilink.entity.chat.TokenUsage;

import java.util.List;

/**
 * 聊天记录服务接口
 */
public interface ChatService {


    /**
     * 保存一条聊天消息到 MySQL
     */
    void saveMessage(String sessionId, String userId,
                     String role, String content, String modelName);

    /**
     * 查询某会话的全部聊天记录
     */
    List<ChatRecord> getSessionHistory(String sessionId);

    /**
     * 查询某用户的全部聊天记录
     */
    List<ChatRecord> getUserHistory(String userId);

    /**
     * 保存 Token 消耗到 MySQL
     */
    void saveTokenUsage(String sessionId, String userId, String modelName,
                        int promptTokens, int completionTokens);

    /**
     * 查询某用户的总 Token 消耗
     */
    int getTotalTokensByUser(String userId);

    /**
     * 追加一条消息到 Redis 上下文
     */
    void appendToContext(String sessionId, String role, String content);

    /**
     * 获取 Redis 中的上下文
     */
    List<String> getContext(String sessionId);

    /**
     * 清空某会话的 Redis 上下文
     */
    void clearContext(String sessionId);

    /**
     * 将 Redis 上下文拼接成 prompt 字符串，实现多轮对话
     */
    String buildPromptWithContext(String sessionId, String userMessage);
}
