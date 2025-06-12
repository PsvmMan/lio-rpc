package com.gt.lio.limiter;

public class RateLimitException extends RuntimeException{

    private static final long serialVersionUID = 1L;

    /**
     * 构造一个带有默认消息的限流异常。
     */
    public RateLimitException() {
        super("RPC request rejected due to rate limiting.");
    }

    /**
     * 构造一个带有自定义消息的限流异常。
     *
     * @param message 详细描述限流原因
     */
    public RateLimitException(String message) {
        super(message);
    }

    /**
     * 构造一个带有原始异常和消息的限流异常。
     *
     * @param message 详细描述限流原因
     * @param cause   触发该异常的原始异常
     */
    public RateLimitException(String message, Throwable cause) {
        super(message, cause);
    }
}
