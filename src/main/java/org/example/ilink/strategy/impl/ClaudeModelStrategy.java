package org.example.ilink.strategy.impl;

import org.example.ilink.strategy.AIModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;


@Component
public class ClaudeModelStrategy implements AIModel {

    @Value("${ai.claude.api-key:}")
    private String apiKey;

    @Value("${ai.claude.model:claude-3-sonnet-20240229}")
    private String model;

    @Value("${ai.claude.api-url:https://api.anthropic.com/v1}")
    private String apiUrl;

    private final WebClient webClient;

    public ClaudeModelStrategy() {
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

            return parseResponse(response);
        } catch (Exception e) {
            return "调用 Claude 失败: " + e.getMessage();
        }
    }

    @Override
    public String getModelName() {
        return model;
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isEmpty();
    }

    private String parseResponse(String response) {
        try {
            int idx = response.indexOf("\"text\":\"");
            if (idx != -1) {
                int start = idx + 8;
                int end = response.indexOf("\"", start);
                return response.substring(start, end);
            }
        } catch (Exception ignored) {}
        return response;
    }
}
