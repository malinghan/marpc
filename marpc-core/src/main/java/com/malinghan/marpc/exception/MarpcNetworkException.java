package com.malinghan.marpc.exception;

/**
 * 网络异常：HTTP 调用失败、响应解析失败等传输层错误。
 */
public class MarpcNetworkException extends MarpcException {

    public MarpcNetworkException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public MarpcNetworkException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
