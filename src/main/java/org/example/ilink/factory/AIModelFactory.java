package org.example.ilink.factory;

import org.example.ilink.strategy.AIModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 模型提供者
 * 负责创建和管理 AI 模型实例
 */
@Component
public class AIModelFactory {
    
    private final Map<String, AIModel> modelRegistry = new HashMap<>();
    
    @Autowired
    public AIModelFactory(List<AIModel> models) {
        // 自动注册所有 AIModel 实现
        for (AIModel model : models) {
            registerModel(model.getModelName(), model);
        }
    }
    
    /**
     * 注册模型
     */
    public void registerModel(String modelName, AIModel model) {
        modelRegistry.put(modelName.toLowerCase(), model);
    }
    
    /**
     * 获取指定模型
     */
    public AIModel getModel(String modelName) {
        return modelRegistry.get(modelName.toLowerCase());
    }
    
    /**
     * 获取第一个可用的模型
     */
    public AIModel getAvailableModel() {
        return modelRegistry.values().stream()
                .filter(AIModel::isAvailable)
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 获取所有已注册的模型名称
     */
    public java.util.Set<String> getAvailableModelNames() {
        return modelRegistry.keySet();
    }
    
    /**
     * 检查模型是否存在
     */
    public boolean hasModel(String modelName) {
        return modelRegistry.containsKey(modelName.toLowerCase());
    }
}
