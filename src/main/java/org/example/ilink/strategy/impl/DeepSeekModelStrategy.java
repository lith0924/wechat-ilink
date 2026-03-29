package org.example.ilink.strategy.impl;

import org.example.ilink.strategy.AIModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;


@Component
public class DeepSeekModelStrategy implements AIModel {

    @Value("${ai.deepseek.api-key:}")
    private String apiKey;

    @Value("${ai.deepseek.model:deepseek-chat}")
    private String model;

    @Value("${ai.deepseek.api-url:https://api.deepseek.com/v1}")
    private String apiUrl;

    private final WebClient webClient;

    public DeepSeekModelStrategy() {
        this.webClient = WebClient.builder().build();
    }

    @Override
    public String generateResponse(String prompt) {
        if (!isAvailable()) {
            return "DeepSeek 模型未配置";
        }
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", new Object[]{
                Map.of("role", "user", "content", prompt)
            });
            requestBody.put("temperature", 0.7);
            requestBody.put("stream", false);

            String response = webClient.post()
                    .uri(apiUrl + "/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseResponse(response);
        } catch (Exception e) {
            return "调用 DeepSeek 失败: " + e.getMessage();
        }
    }

    @Override
    public String getModelName() {
        return "DeepSeek-" + model;
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isEmpty();
    }

    private String parseResponse(String response) {
        try {
            int idx = response.indexOf("\"content\":\"");
            if (idx != -1) {
                int start = idx + 11;
                int end = response.indexOf("\"", start);
                return response.substring(start, end);
            }
        } catch (Exception ignored) {}
        return response;
    }
}
