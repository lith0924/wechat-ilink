package org.example.ilink.strategy.impl;

import org.example.ilink.strategy.AIModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

/**
 * OpenAI 模型策略
 *
 * ============================================================
 * 【Spring AI 替代方案】
 * ============================================================
 * pom.xml 依赖：
 *   <dependency>
 *     <groupId>org.springframework.ai</groupId>
 *     <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
 *     <version>1.0.0</version>
 *   </dependency>
 *
 * application.properties：
 *   spring.ai.openai.api-key=${ai.openai.api-key}
 *   spring.ai.openai.base-url=https://api.openai.com
 *   spring.ai.openai.chat.options.model=gpt-3.5-turbo
 *
 * 替代后的调用方式：
 *   @Autowired
 *   private OpenAiChatModel chatModel;
 *
 *   String response = chatModel
 *       .call(new Prompt("你好"))
 *       .getResult().getOutput().getContent();
 *
 * 替代后这整个类可以删除。
 * ============================================================
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

            return parseResponse(response);
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
