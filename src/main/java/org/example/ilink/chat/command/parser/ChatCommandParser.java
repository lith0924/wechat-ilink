package org.example.ilink.chat.command.parser;

import org.example.ilink.chat.command.model.ChatCommandResult;
import org.example.ilink.chat.command.model.CommandType;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class ChatCommandParser {

    private static final Set<String> SUPPORTED_MODES = Set.of("default", "knowledge", "cosplay");

    public ChatCommandResult parse(String userMessage) {
        if (userMessage == null) {
            return ChatCommandResult.notCommand();
        }

        String trimmed = userMessage.trim();
        if (!trimmed.startsWith("/")) {
            return ChatCommandResult.notCommand();
        }

        if ("/status".equalsIgnoreCase(trimmed)) {
            return ChatCommandResult.success(CommandType.SHOW_STATUS, null);
        }

        if ("/reset".equalsIgnoreCase(trimmed)) {
            return ChatCommandResult.success(CommandType.RESET_SESSION, null);
        }

        String[] parts = trimmed.split("\\s+", 2);
        String command = parts[0].toLowerCase();
        String argument = parts.length > 1 ? parts[1].trim() : "";

        if ("/mode".equals(command)) {
            if (argument.isEmpty()) {
                return ChatCommandResult.invalid(CommandType.SWITCH_MODE,
                        "模式命令格式不正确，请使用 /mode default|knowledge|cosplay");
            }
            String normalizedMode = argument.toLowerCase();
            if (!SUPPORTED_MODES.contains(normalizedMode)) {
                return ChatCommandResult.invalid(CommandType.SWITCH_MODE,
                        "不支持的模式：" + argument + "，可选：default、knowledge、cosplay");
            }
            return ChatCommandResult.success(CommandType.SWITCH_MODE, normalizedMode);
        }

        if ("/model".equals(command)) {
            if (argument.isEmpty()) {
                return ChatCommandResult.invalid(CommandType.SWITCH_MODEL,
                        "模型命令格式不正确，请使用 /model 模型名");
            }
            return ChatCommandResult.success(CommandType.SWITCH_MODEL, argument);
        }

        if ("/prompt".equals(command)) {
            if (argument.isEmpty()) {
                return ChatCommandResult.success(CommandType.SHOW_CUSTOM_PROMPT, null);
            }
            if ("clear".equalsIgnoreCase(argument)) {
                return ChatCommandResult.success(CommandType.CLEAR_CUSTOM_PROMPT, null);
            }
            if ("auto".equalsIgnoreCase(argument)) {
                return ChatCommandResult.success(CommandType.AUTO_GENERATE_CUSTOM_PROMPT, null);
            }
            if (argument.toLowerCase().startsWith("from-chat ")) {
                String payload = argument.substring("from-chat ".length()).trim();
                if (!payload.contains("\n")) {
                    return ChatCommandResult.invalid(CommandType.GENERATE_CUSTOM_PROMPT_FROM_TRANSCRIPT,
                            "命令格式不正确，请使用 /prompt from-chat 说话人名称\\n聊天记录内容");
                }
                return ChatCommandResult.success(CommandType.GENERATE_CUSTOM_PROMPT_FROM_TRANSCRIPT, payload);
            }
            return ChatCommandResult.success(CommandType.SET_CUSTOM_PROMPT, argument);
        }

        return ChatCommandResult.invalid(CommandType.UNKNOWN,
                "不支持的命令，请使用 /mode、/model、/prompt、/status 或 /reset");
    }
}
