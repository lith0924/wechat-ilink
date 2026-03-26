package org.example.ilink.controller;

import org.example.ilink.config.AIConfig;
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

/**
 * AI 控制器
 * 提供 AI 相关的 API 端点
 */
@CrossOrigin
@RestController
@RequestMapping("/api/ai")
public class AIController {
    
    @Autowired
    private AIConfig aiConfig;
    
    /**
     * 使用默认模型生成回复
     */
    @PostMapping("/generate")
    public Result generateResponse(@RequestParam String prompt) {
        String response = aiConfig.generateResponse(prompt);
        Map<String, Object> data = new HashMap<>();
        data.put("response", response);
        return Result.success(data);
    }
    
    /**
     * 使用指定模型生成回复
     */
    @PostMapping("/generate/{modelName}")
    public Result generateResponseWithModel(
            @PathVariable String modelName,
            @RequestParam String prompt) {
        String response = aiConfig.generateResponse(modelName, prompt);
        Map<String, Object> data = new HashMap<>();
        data.put("model", modelName);
        data.put("response", response);
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
