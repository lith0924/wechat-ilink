package org.example.ilink.strategy.impl;

import org.example.ilink.strategy.AIModel;
import org.example.ilink.strategy.AIResponse;
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
        return generateWithUsage(prompt).getContent();
    }

    @Override
    public AIResponse generateWithUsage(String prompt) {
        if (!isAvailable()) {
            return new AIResponse("Claude 模型未配置", 0, 0);
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

            return parseResponseWithUsage(response);
        } catch (Exception e) {
            return new AIResponse("调用 Claude 失败: " + e.getMessage(), 0, 0);
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
            // Claude 响应格式："text":"..."
            String content = "";
            int idx = response.indexOf("\"text\":\"");
            if (idx != -1) {
                int start = idx + 8;
                int end = response.indexOf("\"", start);
                content = response.substring(start, end);
            }
            // 还原 JSON 转义字符
            content = content.replace("\\n", "\n")
                             .replace("\\t", "\t")
                             .replace("\\\"", "\"")
                             .replace("\\\\", "\\");
            // 去除 Markdown 格式符号，微信不支持渲染
            content = content.replaceAll("\\*{1,3}([^*]+)\\*{1,3}", "$1")
                             .replaceAll("#{1,6}\\s*", "")
                             .replaceAll("- ", "")
                             .replaceAll("\\d+\\. ", "")
                             .replaceAll("`{1,3}[^`]*`{1,3}", "");
            // Claude usage 字段："input_tokens", "output_tokens"
            int promptTokens = extractInt(response, "\"input_tokens\":");
            int completionTokens = extractInt(response, "\"output_tokens\":");
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
