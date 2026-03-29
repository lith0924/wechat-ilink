package org.example.ilink.strategy.impl;

import org.example.ilink.strategy.AIModel;
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
        if (!isAvailable()) {
            return "本地模型未启用";
        }
        try {
            return callLocalModel(prompt);
        } catch (Exception e) {
            return "调用本地模型失败: " + e.getMessage();
        }
    }

    @Override
    public String getModelName() {
        return "Local-" + model;
    }

    @Override
    public boolean isAvailable() {
        return enabled;
    }

    private String callLocalModel(String prompt) {
        // 调用 Ollama API：POST http://localhost:11434/api/generate
        // { "model": "llama2", "prompt": "...", "stream": false }
        // Spring AI 替代后可直接删除此方法
        return "本地模型回复: " + prompt;
    }
}
