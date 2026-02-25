package com.malinghan.marpc.core;

import lombok.Data;

/**
 * RPC 响应协议体，统一封装 Provider 的执行结果。
 *
 * <p>使用静态工厂方法构造，Consumer 侧根据 {@code status} 字段判断调用是否成功：
 * 成功时取 {@code data}，失败时取 {@code errorMessage} 并抛出异常。
 */
@Data
public class RpcResponse {
    /** true 表示调用成功，false 表示发生异常或服务/方法未找到 */
    private boolean status;
    /** 方法返回值，JSON 反序列化后为 Object，Consumer 侧按返回类型再次转换 */
    private Object data;
    /** 失败时的错误描述，status=false 时有值 */
    private String errorMessage;

    /** 构造成功响应 */
    public static RpcResponse ok(Object data) {
        RpcResponse r = new RpcResponse();
        r.status = true;
        r.data = data;
        return r;
    }

    /** 构造失败响应 */
    public static RpcResponse error(String message) {
        RpcResponse r = new RpcResponse();
        r.status = false;
        r.errorMessage = message;
        return r;
    }
}