package org.example.ilink.strategy.impl;

import org.example.ilink.strategy.AIModel;
import org.example.ilink.strategy.AIResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LocalModelStrategy implements AIModel {

    @Value("${ai.local.enabled:false}")
    private boolean enabled;

    @Value("${ai.local.model:llama2}")
    private String model;

    @Value("${ai.local.api-url:http://localhost:11434}")
    private String apiUrl;

    @Override
    public String generateResponse(String prompt) {
        return generateWithUsage(prompt).getContent();
    }

    @Override
    public AIResponse generateWithUsage(String prompt) {
        if (!isAvailable()) {
            return new AIResponse("本地模型未启用", 0, 0);
        }
        try {
            String content = callLocalModel(prompt);
            // 本地模型无法获取真实 token，粗估
            int promptTokens = prompt.length() / 2 + 1;
            int completionTokens = content.length() / 2 + 1;
            return new AIResponse(content, promptTokens, completionTokens);
        } catch (Exception e) {
            return new AIResponse("调用本地模型失败: " + e.getMessage(), 0, 0);
        }
    }

    @Override
    public String getModelName() {
        return model;
    }

    @Override
    public boolean isAvailable() {
        return enabled;
    }

    private String callLocalModel(String prompt) {
        // 调用 Ollama API：POST http://localhost:11434/api/generate
        // { "model": "llama2", "prompt": "...", "stream": false }
        return "本地模型回复: " + prompt;
    }
}
