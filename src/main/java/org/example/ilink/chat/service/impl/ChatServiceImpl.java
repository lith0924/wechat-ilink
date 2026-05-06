package org.example.ilink.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.ilink.chat.domain.entity.ChatMessage;
import org.example.ilink.chat.domain.entity.ChatReply;
import org.example.ilink.chat.repository.ChatMessageMapper;
import org.example.ilink.chat.repository.ChatReplyMapper;
import org.example.ilink.chat.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class ChatServiceImpl implements ChatService {

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Autowired
    private ChatReplyMapper chatReplyMapper;

    @Override
    public Long saveMessage(String contextToken, String content, int useAi, String modelName, String chatMode) {
        ChatMessage msg = new ChatMessage();
        msg.setContextToken(contextToken);
        msg.setContent(content);
        msg.setUseAi(useAi);
        msg.setModelName(useAi == 1 ? modelName : null);
        msg.setChatMode(chatMode != null ? chatMode : "default");
        msg.setCreatedAt(LocalDateTime.now());
        chatMessageMapper.insert(msg);
        return msg.getId();
    }

    @Override
    public void saveReply(Long messageId, String content, int totalTokens) {
        ChatReply reply = new ChatReply();
        reply.setMessageId(messageId);
        reply.setContent(content);
        reply.setTotalTokens(totalTokens);
        reply.setCreatedAt(LocalDateTime.now());
        chatReplyMapper.insert(reply);
    }

    @Override
    public List<ChatMessage> getMessagesByContextToken(String contextToken) {
        return chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getContextToken, contextToken)
                        .orderByAsc(ChatMessage::getCreatedAt)
        );
    }

    @Override
    public List<ChatReply> getRepliesByMessageId(Long messageId) {
        return chatReplyMapper.selectList(
                new LambdaQueryWrapper<ChatReply>()
                        .eq(ChatReply::getMessageId, messageId)
                        .orderByAsc(ChatReply::getCreatedAt)
        );
    }

    @Override
    public List<ChatMessage> getRecentAiMessagesByContextToken(String contextToken, int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }
        List<ChatMessage> messages = chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getContextToken, contextToken)
                        .eq(ChatMessage::getUseAi, 1)
                        .notLikeRight(ChatMessage::getContent, "/")
                        .orderByDesc(ChatMessage::getCreatedAt)
                        .last("LIMIT " + limit)
        );
        Collections.reverse(messages);
        return messages;
    }
}
