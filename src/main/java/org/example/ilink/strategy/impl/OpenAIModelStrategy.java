package org.example.ilink.strategy.impl;

import org.example.ilink.strategy.AIModel;
import org.example.ilink.strategy.AIResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

/**
 * OpenAI 模型策略
 */
@Component
public class OpenAIModelStrategy implements AIModel {

    @Value("${ai.openai.api-key:}")
    private String apiKey;

    @Value("${ai.openai.model:gpt-3.5-turbo}")
    private String model;

    @Value("${ai.openai.api-url:https://api.openai.com/v1}")
    private String apiUrl;

    private final WebClient webClient;

    public OpenAIModelStrategy() {
        this.webClient = WebClient.builder().build();
    }

    @Override
    public String generateResponse(String prompt) {
        return generateWithUsage(prompt).getContent();
    }

    @Override
    public AIResponse generateWithUsage(String prompt) {
        if (!isAvailable()) {
            return new AIResponse("OpenAI 模型未配置", 0, 0);
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

            return parseResponseWithUsage(response);
        } catch (Exception e) {
            return new AIResponse("调用 OpenAI 失败: " + e.getMessage(), 0, 0);
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

    private AIResponse parseResponseWithUsage(String response) {
        try {
            String content = "";
            int idx = response.indexOf("\"content\":\"");
            if (idx != -1) {
                int start = idx + 11;
                int end = response.indexOf("\"", start);
                content = response.substring(start, end);
            }
            content = content.replace("\\n", "\n")
                             .replace("\\t", "\t")
                             .replace("\\\"", "\"")
                             .replace("\\\\", "\\");
            content = content.replaceAll("\\*{1,3}([^*]+)\\*{1,3}", "$1")
                             .replaceAll("#{1,6}\\s*", "")
                             .replaceAll("- ", "")
                             .replaceAll("\\d+\\. ", "")
                             .replaceAll("`{1,3}[^`]*`{1,3}", "");
            int promptTokens = extractInt(response, "\"prompt_tokens\":");
            int completionTokens = extractInt(response, "\"completion_tokens\":");
            return new AIResponse(content, promptTokens, completionTokens);
        } catch (Exception e) {
            return new AIResponse(response, 0, 0);
        }
    }

    private int extractInt(String json, String key) {
        try {
            int idx = json.indexOf(key);
            if (idx == -1) return 0;
            int start = idx + key.length();
            int end = start;
            while (end < json.length() && Character.isDigit(json.charAt(end))) end++;
            return Integer.parseInt(json.substring(start, end));
        } catch (Exception e) {
            return 0;
        }
    }
}
