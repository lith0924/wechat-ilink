package org.example.ilink.model.impl;

import org.example.ilink.model.AIModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

/**
 * OpenAI 模型实现
 */
@Component
public class OpenAIModel implements AIModel {
    
    @Value("${ai.openai.api-key:}")
    private String apiKey;
    
    @Value("${ai.openai.model:gpt-3.5-turbo}")
    private String model;
    
    @Value("${ai.openai.api-url:https://api.openai.com/v1}")
    private String apiUrl;
    
    private final WebClient webClient;
    
    public OpenAIModel() {
        this.webClient = WebClient.builder().build();
    }
    
    @Override
    public String generateResponse(String prompt) {
        if (!isAvailable()) {
            return "OpenAI 模型未配置";
        }
        
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", new Object[]{
                Map.of("role", "user", "content", prompt)
            });
            requestBody.put("temperature", 0.7);
            
            String response = webClient.post()
                    .uri(apiUrl + "/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            return parseOpenAIResponse(response);
            
        } catch (Exception e) {
            return "调用 OpenAI 失败: " + e.getMessage();
        }
    }
    
    @Override
    public String getModelName() {
        return "OpenAI-" + model;
    }
    
    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isEmpty();
    }
    
    private String parseOpenAIResponse(String response) {
        try {
            int contentIndex = response.indexOf("\"content\":\"");
            if (contentIndex != -1) {
                int startIndex = contentIndex + 11;
                int endIndex = response.indexOf("\"", startIndex);
                return response.substring(startIndex, endIndex);
            }
        } catch (Exception e) {
            // 解析失败
        }
        return response;
    }
}
