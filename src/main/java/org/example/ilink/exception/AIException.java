package org.example.ilink.exception;

/**
 * AI 模型异常
 */
public class AIException extends RuntimeException {
    
    private String errorCode;
    private String errorMessage;
    
    public AIException(String message) {
        super(message);
        this.errorMessage = message;
    }
    
    public AIException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.errorMessage = message;
    }
    
    public AIException(String message, Throwable cause) {
        super(message, cause);
        this.errorMessage = message;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
}
