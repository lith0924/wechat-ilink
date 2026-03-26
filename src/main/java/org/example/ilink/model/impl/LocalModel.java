package org.example.ilink.model.impl;

import org.example.ilink.model.AIModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 本地模型实现示例
 * 可以用于集成本地 LLM（如 Ollama、LM Studio 等）
 */
@Component
public class LocalModel implements AIModel {
    
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
        // 实现具体的本地模型调用逻辑
        // 示例：调用 Ollama API
        // POST http://localhost:11434/api/generate
        // {
        //   "model": "llama2",
        //   "prompt": "...",
        //   "stream": false
        // }
        return "本地模型回复: " + prompt;
    }
}
