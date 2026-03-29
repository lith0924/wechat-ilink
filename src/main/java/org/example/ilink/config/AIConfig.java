package org.example.ilink.config;

import org.example.ilink.factory.AIModelFactory;
import org.example.ilink.strategy.AIModel;
import org.example.ilink.strategy.AIResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * AI 配置和服务
 * 提供统一的 AI 调用接口
 */
@Component
public class AIConfig {
    
    @Autowired
    private AIModelFactory modelProvider;
    
    @Value("${ai.default-model:}")
    private String defaultModel;
    
    /**
     * 使用默认模型生成回复（纯文本）
     */
    public String generateResponse(String prompt) {
        AIModel model = getActiveModel();
        if (model == null) {
            return "没有可用的 AI 模型";
        }
        return model.generateResponse(prompt);
    }

    /**
     * 使用默认模型生成回复，返回包含真实 token 数的 AIResponse
     */
    public AIResponse generateWithUsage(String prompt) {
        AIModel model = getActiveModel();
        if (model == null) {
            return new AIResponse("没有可用的 AI 模型", 0, 0);
        }
        return model.generateWithUsage(prompt);
    }
    
    /**
     * 使用指定模型生成回复
     */
    public String generateResponse(String modelName, String prompt) {
        AIModel model = modelProvider.getModel(modelName);
        if (model == null) {
            return "模型 " + modelName + " 不存在";
        }
        if (!model.isAvailable()) {
            return "模型 " + modelName + " 未配置或不可用";
        }
        return model.generateResponse(prompt);
    }

    /**
     * 获取当前默认模型名称
     */
    public String getDefaultModelName() {
        AIModel model = getActiveModel();
        return model != null ? model.getModelName() : "unknown";
    }
    
    /**
     * 获取当前活跃的模型
     */
    private AIModel getActiveModel() {
        if (defaultModel != null && !defaultModel.isEmpty()) {
            AIModel model = modelProvider.getModel(defaultModel);
            if (model != null && model.isAvailable()) {
                return model;
            }
        }
        return modelProvider.getAvailableModel();
    }
    
    /**
     * 获取所有可用的模型名称
     */
    public java.util.Set<String> getAvailableModels() {
        return modelProvider.getAvailableModelNames();
    }

    public AIModelFactory getModelProvider() {
        return modelProvider;
    }
}
