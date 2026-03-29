package org.example.ilink.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.ilink.entity.chat.ChatRecord;
import org.example.ilink.entity.chat.TokenUsage;
import org.example.ilink.mapper.ChatRecordMapper;
import org.example.ilink.mapper.TokenUsageMapper;
import org.example.ilink.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 聊天记录 Service 实现类
 */
@Service
public class ChatServiceImpl implements ChatService {

    private static final String CONTEXT_KEY_PREFIX = "chat:context:";
    private static final int MAX_CONTEXT_SIZE = 10;
    private static final long CONTEXT_TTL_MINUTES = 30;

    @Autowired
    private ChatRecordMapper chatRecordMapper;

    @Autowired
    private TokenUsageMapper tokenUsageMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public void saveMessage(String sessionId, String userId,
                            String role, String content, String modelName) {
        ChatRecord record = new ChatRecord();
        record.setSessionId(sessionId);
        record.setUserId(userId);
        record.setRole(role);
        record.setContent(content);
        record.setModelName(modelName);
        record.setCreatedAt(LocalDateTime.now());
        chatRecordMapper.insert(record);
    }

    @Override
    public List<ChatRecord> getSessionHistory(String sessionId) {
        return chatRecordMapper.selectList(
                new LambdaQueryWrapper<ChatRecord>()
                        .eq(ChatRecord::getSessionId, sessionId)
                        .orderByAsc(ChatRecord::getCreatedAt)
        );
    }

    @Override
    public List<ChatRecord> getUserHistory(String userId) {
        return chatRecordMapper.selectList(
                new LambdaQueryWrapper<ChatRecord>()
                        .eq(ChatRecord::getUserId, userId)
                        .orderByAsc(ChatRecord::getCreatedAt)
        );
    }


    @Override
    public void saveTokenUsage(String sessionId, String userId, String modelName,
                               int promptTokens, int completionTokens) {
        TokenUsage usage = new TokenUsage();
        usage.setSessionId(sessionId);
        usage.setUserId(userId);
        usage.setModelName(modelName);
        usage.setPromptTokens(promptTokens);
        usage.setCompletionTokens(completionTokens);
        usage.setTotalTokens(promptTokens + completionTokens);
        usage.setCreatedAt(LocalDateTime.now());
        tokenUsageMapper.insert(usage);
    }

    @Override
    public int getTotalTokensByUser(String userId) {
        List<TokenUsage> list = tokenUsageMapper.selectList(
                new LambdaQueryWrapper<TokenUsage>()
                        .eq(TokenUsage::getUserId, userId)
        );
        return list.stream().mapToInt(TokenUsage::getTotalTokens).sum();
    }


    @Override
    public void appendToContext(String sessionId, String role, String content) {
        String key = CONTEXT_KEY_PREFIX + sessionId;
        String entry = role + ":" + content.replace("\n", " ");
        redisTemplate.opsForList().rightPush(key, entry);
        redisTemplate.opsForList().trim(key, -MAX_CONTEXT_SIZE, -1);
        redisTemplate.expire(key, CONTEXT_TTL_MINUTES, TimeUnit.MINUTES);
    }

    @Override
    public List<String> getContext(String sessionId) {
        String key = CONTEXT_KEY_PREFIX + sessionId;
        List<String> context = redisTemplate.opsForList().range(key, 0, -1);
        return context != null ? context : List.of();
    }

    @Override
    public void clearContext(String sessionId) {
        redisTemplate.delete(CONTEXT_KEY_PREFIX + sessionId);
    }

    @Override
    public String buildPromptWithContext(String sessionId, String userMessage) {
        List<String> context = getContext(sessionId);
        if (context.isEmpty()) {
            return userMessage;
        }
        StringBuilder sb = new StringBuilder();
        for (String entry : context) {
            String[] parts = entry.split(":", 2);
            if (parts.length == 2) {
                sb.append(parts[0]).append(": ").append(parts[1]).append("\n");
            }
        }
        sb.append("user: ").append(userMessage);
        return sb.toString();
    }
}
