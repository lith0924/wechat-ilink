package org.example.ilink.strategy;

/**
 * AI 响应结果，包含回复文本和真实 token 消耗
 */
public class AIResponse {

    private String content;
    private int promptTokens;
    private int completionTokens;

    public AIResponse(String content, int promptTokens, int completionTokens) {
        this.content = content;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
    }

    public String getContent() { return content; }
    public int getPromptTokens() { return promptTokens; }
    public int getCompletionTokens() { return completionTokens; }
    public int getTotalTokens() { return promptTokens + completionTokens; }
}
