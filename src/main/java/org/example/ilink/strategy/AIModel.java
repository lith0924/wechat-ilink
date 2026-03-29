package org.example.ilink.strategy;
public interface AIModel {

    /**
     * 调用 AI 模型生成回复
     * @param prompt 用户输入的提示词
     * @return AI 模型的回复内容
     */
    String generateResponse(String prompt);

    /**
     * 调用 AI 模型生成回复
     * @param prompt 用户输入的提示词
     * @return AIResponse 包含回复文本和 token 数
     */
    default AIResponse generateWithUsage(String prompt) {
        String content = generateResponse(prompt);
        // 默认粗估，子类可覆盖以返回真实值
        int promptTokens = prompt.length() / 2 + 1;
        int completionTokens = content.length() / 2 + 1;
        return new AIResponse(content, promptTokens, completionTokens);
    }

    /**
     * 获取模型名称
     * @return 模型名称
     */
    String getModelName();

    /**
     * 检查模型是否可用（API Key 是否已配置）
     * @return true 表示可用，false 表示不可用
     */
    boolean isAvailable();
}
