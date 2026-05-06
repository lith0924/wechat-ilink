package org.example.ilink.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.example.ilink.factory.AIModelFactory;
import org.example.ilink.strategy.AIModel;
import org.example.ilink.strategy.AIResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * AI 配置和服务
 * 提供统一的 AI 调用接口
 *
 * 说明：
 * 1. 当前聊天主链路已迁移到 langchain4j；
 * 2. 现有策略模式调用入口暂时保留，用于继续支持多模型切换；
 * 3. 等未来确认可以统一 provider 抽象后，再考虑彻底合并两层实现。
 */
@Component
public class AIConfig {

    @Autowired
    private AIModelFactory modelProvider;

    @Value("${ai.default-model:}")
    private String defaultModel;

    @Value("${ai.qianwen.api-key:}")
    private String qwenApiKey;

    @Value("${ai.qianwen.api-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String qwenApiUrl;

    @Value("${ai.deepseek.api-key:}")
    private String deepseekApiKey;

    @Value("${ai.deepseek.api-url:https://api.deepseek.com}")
    private String deepseekApiUrl;

    @Value("${ai.openai.api-key:}")
    private String openAiApiKey;

    @Value("${ai.openai.api-url:https://api.openai.com/v1}")
    private String openAiApiUrl;

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
     * 使用指定模型生成回复，返回包含 token 数的 AIResponse
     */
    public AIResponse generateWithUsage(String modelName, String prompt) {
        AIModel model = modelProvider.getModel(modelName);
        if (model == null) {
            return new AIResponse("模型 " + modelName + " 不存在", 0, 0);
        }
        if (!model.isAvailable()) {
            return new AIResponse("模型 " + modelName + " 未配置或不可用", 0, 0);
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

    public ChatLanguageModel getChatLanguageModel(String modelName) {
        String resolvedModel = modelName == null || modelName.isBlank() ? getDefaultModelName() : modelName;
        if (resolvedModel.startsWith("qwen")) {
            return buildOpenAiCompatibleModel(qwenApiKey, qwenApiUrl, resolvedModel);
        }
        if (resolvedModel.startsWith("deepseek")) {
            return buildOpenAiCompatibleModel(deepseekApiKey, deepseekApiUrl, resolvedModel);
        }
        return buildOpenAiCompatibleModel(openAiApiKey, openAiApiUrl, resolvedModel);
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

    private ChatLanguageModel buildOpenAiCompatibleModel(String apiKey, String baseUrl, String modelName) {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .build();
    }
}
