package org.example.ilink.model.impl;

import org.example.ilink.model.AIModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 通义千问模型实现
 * 阿里云 AI 模型
 */
@Component
public class QianWenModel implements AIModel {
    
    @Value("${ai.qianwen.api-key:}")
    private String apiKey;
    
    @Value("${ai.qianwen.model:qwen-turbo}")
    private String model;
    
    @Value("${ai.qianwen.api-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String apiUrl;
    
    private final org.springframework.web.reactive.function.client.WebClient webClient;
    
    public QianWenModel() {
        this.webClient = org.springframework.web.reactive.function.client.WebClient.builder().build();
    }
    
    @Override
    public String generateResponse(String prompt) {
        if (!isAvailable()) {
            return "通义千问模型未配置";
        }
        
        try {
            java.util.Map<String, Object> requestBody = new java.util.HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", new Object[]{
                java.util.Map.of("role", "user", "content", prompt)
            });
            
            String response = webClient.post()
                    .uri(apiUrl + "/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            return parseQianWenResponse(response);
            
        } catch (Exception e) {
            return "调用通义千问失败: " + e.getMessage();
        }
    }
    
    @Override
    public String getModelName() {
        return "QianWen-" + model;
    }
    
    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isEmpty();
    }
    
    private String parseQianWenResponse(String response) {
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
