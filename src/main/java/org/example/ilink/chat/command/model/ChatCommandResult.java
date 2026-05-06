package org.example.ilink.chat.command.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatCommandResult {

    private boolean command;
    private CommandType commandType;
    private String argument;
    private boolean success;
    private String message;

    public static ChatCommandResult notCommand() {
        ChatCommandResult result = new ChatCommandResult();
        result.setCommand(false);
        result.setCommandType(CommandType.UNKNOWN);
        result.setSuccess(false);
        return result;
    }

    public static ChatCommandResult success(CommandType commandType, String argument) {
        ChatCommandResult result = new ChatCommandResult();
        result.setCommand(true);
        result.setCommandType(commandType);
        result.setArgument(argument);
        result.setSuccess(true);
        return result;
    }

    public static ChatCommandResult invalid(CommandType commandType, String message) {
        ChatCommandResult result = new ChatCommandResult();
        result.setCommand(true);
        result.setCommandType(commandType);
        result.setSuccess(false);
        result.setMessage(message);
        return result;
    }
}
