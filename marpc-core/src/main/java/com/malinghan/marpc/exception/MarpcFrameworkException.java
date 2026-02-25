package com.malinghan.marpc.exception;

/**
 * 框架异常：注册失败、代理注入失败、无可用实例等框架内部错误。
 */
public class MarpcFrameworkException extends MarpcException {

    public MarpcFrameworkException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public MarpcFrameworkException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
