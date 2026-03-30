package org.example.ilink.controller;

import org.example.ilink.config.AIConfig;
import org.example.ilink.service.ChatService;
import org.example.ilink.strategy.AIResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.example.ilink.utils.Result;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@CrossOrigin
@RestController
@RequestMapping("/api/ai")
public class AIController {
    
    @Autowired
    private AIConfig aiConfig;

    @Autowired
    private ChatService chatService;
    
    /**
     * 使用默认模型生成回复
     */
    @PostMapping("/generate")
    public Result generateResponse(
            @RequestParam String prompt,
            @RequestParam(required = false) String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }
        AIResponse aiResult = aiConfig.generateWithUsage(prompt);
        Long messageId = chatService.saveMessage(sessionId, prompt, 1, aiConfig.getDefaultModelName(), "default");
        chatService.saveReply(messageId, aiResult.getContent(), aiResult.getTotalTokens());
        Map<String, Object> data = new HashMap<>();
        data.put("response", aiResult.getContent());
        data.put("sessionId", sessionId);
        data.put("totalTokens", aiResult.getTotalTokens());
        return Result.success(data);
    }
    
    /**
     * 使用指定模型生成回复
     */
    @PostMapping("/generate/{modelName}")
    public Result generateResponseWithModel(
            @PathVariable String modelName,
            @RequestParam String prompt,
            @RequestParam(required = false) String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }
        String response = aiConfig.generateResponse(modelName, prompt);
        Long messageId = chatService.saveMessage(sessionId, prompt, 1, modelName, "default");
        int totalTokens = (prompt.length() + response.length()) / 2 + 1;
        chatService.saveReply(messageId, response, totalTokens);
        Map<String, Object> data = new HashMap<>();
        data.put("model", modelName);
        data.put("response", response);
        data.put("sessionId", sessionId);
        return Result.success(data);
    }
    
    /**
     * 获取所有可用的模型
     */
    @GetMapping("/models")
    public Result getAvailableModels() {
        Map<String, Object> data = new HashMap<>();
        data.put("models", aiConfig.getAvailableModels());
        return Result.success(data);
    }
}
