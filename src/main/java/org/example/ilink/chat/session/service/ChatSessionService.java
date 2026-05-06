package org.example.ilink.chat.session.service;

import org.example.ilink.chat.session.domain.ChatSessionState;
import org.example.ilink.chat.session.repository.ChatSessionRepository;
import org.example.ilink.config.AIConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ChatSessionService {

    @Autowired
    private ChatSessionRepository chatSessionRepository;

    @Autowired
    private AIConfig aiConfig;

    public ChatSessionState getSession(String contextToken) {
        ChatSessionState state = chatSessionRepository.findByContextToken(contextToken);
        if (state != null) {
            return state;
        }
        return buildDefaultSession(contextToken);
    }

    public ChatSessionState updateMode(String contextToken, String mode) {
        ChatSessionState state = getSession(contextToken);
        state.setMode(mode);
        state.setUpdatedAt(LocalDateTime.now());
        chatSessionRepository.save(state);
        return state;
    }

    public ChatSessionState updateModel(String contextToken, String model) {
        ChatSessionState state = getSession(contextToken);
        state.setModel(model);
        state.setUpdatedAt(LocalDateTime.now());
        chatSessionRepository.save(state);
        return state;
    }

    public ChatSessionState updateCustomPrompt(String contextToken, String customPrompt) {
        ChatSessionState state = getSession(contextToken);
        state.setCustomPrompt(customPrompt);
        state.setUpdatedAt(LocalDateTime.now());
        chatSessionRepository.save(state);
        return state;
    }

    public ChatSessionState clearCustomPrompt(String contextToken) {
        return updateCustomPrompt(contextToken, null);
    }

    public ChatSessionState updateStyleTranscript(String contextToken, String styleTranscript) {
        ChatSessionState state = getSession(contextToken);
        state.setStyleTranscript(styleTranscript);
        state.setUpdatedAt(LocalDateTime.now());
        chatSessionRepository.save(state);
        return state;
    }

    public ChatSessionState clearStyleTranscript(String contextToken) {
        return updateStyleTranscript(contextToken, null);
    }

    public ChatSessionState reset(String contextToken) {
        chatSessionRepository.deleteByContextToken(contextToken);
        return buildDefaultSession(contextToken);
    }

    private ChatSessionState buildDefaultSession(String contextToken) {
        return new ChatSessionState(
                contextToken,
                "default",
                aiConfig.getDefaultModelName(),
                null,
                null,
                LocalDateTime.now()
        );
    }
}
