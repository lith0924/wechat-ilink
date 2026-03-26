package org.example.ilink.config;

import org.example.ilink.factory.AIModelFactory;
import org.example.ilink.model.AIModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * AI 配置和服务
 * 提供统一的 AI 调用接口
 * 
 * 这是一个全局的服务类，所有模块都可以注入使用
 */
@Component
public class AIConfig {
    
    @Autowired
    private AIModelFactory modelProvider;
    
    @Value("${ai.default-model:}")
    private String defaultModel;
    
    /**
     * 使用默认模型生成回复
     */
    public String generateResponse(String prompt) {
        AIModel model = getActiveModel();
        if (model == null) {
            return "没有可用的 AI 模型";
        }
        return model.generateResponse(prompt);
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
     * 获取当前活跃的模型
     */
    private AIModel getActiveModel() {
        // 优先使用配置的默认模型
        if (defaultModel != null && !defaultModel.isEmpty()) {
            AIModel model = modelProvider.getModel(defaultModel);
            if (model != null && model.isAvailable()) {
                return model;
            }
        }
        // 否则使用第一个可用的模型
        return modelProvider.getAvailableModel();
    }
    
    /**
     * 获取所有可用的模型名称
     */
    public java.util.Set<String> getAvailableModels() {
        return modelProvider.getAvailableModelNames();
    }
    
    /**
     * 获取模型提供者（如果需要直接操作）
     */
    public AIModelFactory getModelProvider() {
        return modelProvider;
    }
}
