package org.example.ilink.chat.command.service;

import org.example.ilink.chat.command.model.ChatCommandResult;
import org.example.ilink.chat.command.model.CommandType;
import org.example.ilink.chat.service.ChatStylePromptService;
import org.example.ilink.chat.service.impl.ChatAppServiceImpl;
import org.example.ilink.chat.session.domain.ChatSessionState;
import org.example.ilink.chat.session.service.ChatSessionService;
import org.example.ilink.config.AIConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ChatCommandService {

    @Autowired
    private ChatSessionService chatSessionService;

    @Autowired
    private ChatAppServiceImpl chatAppService;

    @Autowired
    private ChatStylePromptService chatStylePromptService;

    @Autowired
    private AIConfig aiConfig;

    public String handle(String contextToken, ChatCommandResult commandResult) {
        if (!commandResult.isSuccess()) {
            return commandResult.getMessage();
        }

        if (commandResult.getCommandType() == CommandType.SWITCH_MODE) {
            ChatSessionState state = chatSessionService.updateMode(contextToken, commandResult.getArgument());
            return "已切换模式为 " + state.getMode() + "，当前模型为 " + state.getModel();
        }

        if (commandResult.getCommandType() == CommandType.SWITCH_MODEL) {
            String modelName = commandResult.getArgument();
            if (!aiConfig.getModelProvider().hasModel(modelName)) {
                return "模型不存在：" + modelName + "，可选模型：" + String.join("、", aiConfig.getAvailableModels());
            }
            ChatSessionState state = chatSessionService.updateModel(contextToken, modelName);
            return "已切换模型为 " + state.getModel() + "，当前模式为 " + state.getMode();
        }

        if (commandResult.getCommandType() == CommandType.SET_CUSTOM_PROMPT) {
            ChatSessionState state = chatSessionService.updateCustomPrompt(contextToken, commandResult.getArgument());
            chatAppService.clearMemory(contextToken);
            return "已设置自定义风格 prompt：" + summarizePrompt(state.getCustomPrompt());
        }

        if (commandResult.getCommandType() == CommandType.AUTO_GENERATE_CUSTOM_PROMPT) {
            ChatSessionState currentState = chatSessionService.getSession(contextToken);
            String generatedPrompt = chatStylePromptService.generatePromptFromRecentMessages(contextToken, currentState.getModel());
            if (generatedPrompt == null || generatedPrompt.isBlank()) {
                return "当前聊天记录不足，暂时无法自动生成风格 prompt。请先多进行几轮普通对话后再试。";
            }
            ChatSessionState state = chatSessionService.updateCustomPrompt(contextToken, generatedPrompt);
            chatAppService.clearMemory(contextToken);
            return "已根据最近聊天记录自动生成风格 prompt：" + state.getCustomPrompt();
        }

        if (commandResult.getCommandType() == CommandType.GENERATE_CUSTOM_PROMPT_FROM_TRANSCRIPT) {
            ChatSessionState currentState = chatSessionService.getSession(contextToken);
            String[] payloadParts = splitTranscriptPayload(commandResult.getArgument());
            if (payloadParts == null) {
                return "命令格式不正确，请使用 /prompt from-chat 说话人名称\\n聊天记录内容";
            }
            String generatedPrompt = chatStylePromptService.generatePromptFromTranscript(
                    payloadParts[0],
                    payloadParts[1],
                    currentState.getModel()
            );
            if (generatedPrompt == null || generatedPrompt.isBlank()) {
                return "未能从这段聊天记录中提炼出稳定风格，请补充更多目标说话人的聊天内容后再试。";
            }
            ChatSessionState state = chatSessionService.updateCustomPrompt(contextToken, generatedPrompt);
            chatAppService.clearMemory(contextToken);
            return "已根据聊天记录为“" + payloadParts[0] + "”生成风格 prompt：" + state.getCustomPrompt();
        }

        if (commandResult.getCommandType() == CommandType.SHOW_CUSTOM_PROMPT) {
            ChatSessionState state = chatSessionService.getSession(contextToken);
            if (state.getCustomPrompt() == null || state.getCustomPrompt().isBlank()) {
                return "当前未设置自定义风格 prompt。可使用 /prompt 你的风格要求 进行设置，/prompt auto 自动生成，/prompt from-chat 说话人名称\\n聊天记录内容 进行分析，或 /prompt clear 清空。";
            }
            return "当前自定义风格 prompt：" + state.getCustomPrompt();
        }

        if (commandResult.getCommandType() == CommandType.CLEAR_CUSTOM_PROMPT) {
            ChatSessionState state = chatSessionService.clearCustomPrompt(contextToken);
            chatAppService.clearMemory(contextToken);
            return "已清空自定义风格 prompt，当前模式为 " + state.getMode() + "，当前模型为 " + state.getModel();
        }

        if (commandResult.getCommandType() == CommandType.SHOW_STATUS) {
            ChatSessionState state = chatSessionService.getSession(contextToken);
            String promptStatus = (state.getCustomPrompt() == null || state.getCustomPrompt().isBlank())
                    ? "未设置"
                    : summarizePrompt(state.getCustomPrompt());
            return "当前会话状态：mode=" + state.getMode()
                    + "，model=" + state.getModel()
                    + "，prompt=" + promptStatus;
        }

        if (commandResult.getCommandType() == CommandType.RESET_SESSION) {
            ChatSessionState state = chatSessionService.reset(contextToken);
            chatAppService.clearMemory(contextToken);
            return "已恢复默认状态：mode=" + state.getMode() + "，model=" + state.getModel();
        }

        return "不支持的命令，请使用 /mode、/model、/prompt、/status 或 /reset";
    }

    private String summarizePrompt(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return "未设置";
        }
        return prompt.length() <= 40 ? prompt : prompt.substring(0, 40) + "...";
    }

    private String[] splitTranscriptPayload(String payload) {
        if (payload == null || payload.isBlank()) {
            return null;
        }
        String normalized = payload.replace("\r\n", "\n");
        int splitIndex = normalized.indexOf('\n');
        if (splitIndex <= 0 || splitIndex >= normalized.length() - 1) {
            return null;
        }
        String targetSpeaker = normalized.substring(0, splitIndex).trim();
        String transcript = normalized.substring(splitIndex + 1).trim();
        if (targetSpeaker.isBlank() || transcript.isBlank()) {
            return null;
        }
        return new String[]{targetSpeaker, transcript};
    }
}
