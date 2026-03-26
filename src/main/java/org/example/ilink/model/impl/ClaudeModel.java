package org.example.ilink.model.impl;

import org.example.ilink.model.AIModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Claude 模型实现
 */
@Component
public class ClaudeModel implements AIModel {
    
    @Value("${ai.claude.api-key:}")
    private String apiKey;
    
    @Value("${ai.claude.model:claude-3-sonnet-20240229}")
    private String model;
    
    @Value("${ai.claude.api-url:https://api.anthropic.com/v1}")
    private String apiUrl;
    
    private final WebClient webClient;
    
    public ClaudeModel() {
        this.webClient = WebClient.builder().build();
    }
    
    @Override
    public String generateResponse(String prompt) {
        if (!isAvailable()) {
            return "Claude 模型未配置";
        }
        
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("max_tokens", 1024);
            requestBody.put("messages", new Object[]{
                Map.of("role", "user", "content", prompt)
            });
            
            String response = webClient.post()
                    .uri(apiUrl + "/messages")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            return parseClaudeResponse(response);
            
        } catch (Exception e) {
            return "调用 Claude 失败: " + e.getMessage();
        }
    }
    
    @Override
    public String getModelName() {
        return "Claude-" + model;
    }
    
    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isEmpty();
    }
    
    private String parseClaudeResponse(String response) {
        try {
            int textIndex = response.indexOf("\"text\":\"");
            if (textIndex != -1) {
                int startIndex = textIndex + 8;
                int endIndex = response.indexOf("\"", startIndex);
                return response.substring(startIndex, endIndex);
            }
        } catch (Exception e) {
            // 解析失败
        }
        return response;
    }
}
