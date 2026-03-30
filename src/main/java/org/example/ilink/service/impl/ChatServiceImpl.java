package org.example.ilink.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.ilink.entity.chat.ChatMessage;
import org.example.ilink.entity.chat.ChatReply;
import org.example.ilink.mapper.ChatMessageMapper;
import org.example.ilink.mapper.ChatReplyMapper;
import org.example.ilink.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
}
