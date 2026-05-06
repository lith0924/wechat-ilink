package org.example.ilink.chat.session.domain;

import java.time.LocalDateTime;

public class ChatSessionState {

    private String contextToken;
    private String mode;
    private String model;
    private String customPrompt;
    private String styleTranscript;
    private LocalDateTime updatedAt;

    public ChatSessionState() {
    }

    public ChatSessionState(String contextToken, String mode, String model, String customPrompt, String styleTranscript, LocalDateTime updatedAt) {
        this.contextToken = contextToken;
        this.mode = mode;
        this.model = model;
        this.customPrompt = customPrompt;
        this.styleTranscript = styleTranscript;
        this.updatedAt = updatedAt;
    }

    public String getContextToken() {
        return contextToken;
    }

    public void setContextToken(String contextToken) {
        this.contextToken = contextToken;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getCustomPrompt() {
        return customPrompt;
    }

    public void setCustomPrompt(String customPrompt) {
        this.customPrompt = customPrompt;
    }

    public String getStyleTranscript() {
        return styleTranscript;
    }

    public void setStyleTranscript(String styleTranscript) {
        this.styleTranscript = styleTranscript;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
