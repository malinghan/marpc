package com.malinghan.marpc.exception;

/**
 * 业务异常：服务未找到、方法未找到等调用层面的错误。
 */
public class MarpcBizException extends MarpcException {

    public MarpcBizException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public MarpcBizException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
