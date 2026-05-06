package org.example.ilink.chat.session.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.ilink.chat.session.domain.ChatSessionState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
public class ChatSessionRepository {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${chat.session.prefix:chat:session:}")
    private String sessionPrefix;

    @Value("${chat.session.ttl-hours:72}")
    private long sessionTtlHours;

    public ChatSessionState findByContextToken(String contextToken) {
        String value = stringRedisTemplate.opsForValue().get(buildKey(contextToken));
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(value, ChatSessionState.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("读取会话状态失败", e);
        }
    }

    public void save(ChatSessionState state) {
        try {
            String value = objectMapper.writeValueAsString(state);
            stringRedisTemplate.opsForValue().set(
                    buildKey(state.getContextToken()),
                    value,
                    Duration.ofHours(sessionTtlHours)
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("保存会话状态失败", e);
        }
    }

    public void deleteByContextToken(String contextToken) {
        stringRedisTemplate.delete(buildKey(contextToken));
    }

    private String buildKey(String contextToken) {
        return sessionPrefix + contextToken;
    }
}
