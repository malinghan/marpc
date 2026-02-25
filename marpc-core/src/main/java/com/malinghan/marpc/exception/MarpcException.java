package com.malinghan.marpc.exception;

/**
 * marpc 框架统一异常基类。
 * 子类按异常来源分为三类：业务异常、框架异常、网络异常。
 */
public class MarpcException extends RuntimeException {

    private final ErrorCode errorCode;

    public MarpcException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public MarpcException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public enum ErrorCode {
        // 业务异常
        SERVICE_NOT_FOUND,
        METHOD_NOT_FOUND,
        // 框架异常
        PROVIDER_REGISTER_FAILED,
        CONSUMER_INJECT_FAILED,
        NO_AVAILABLE_INSTANCE,
        // 网络异常
        NETWORK_ERROR,
        RESPONSE_PARSE_ERROR,
    }
}
