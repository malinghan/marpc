package com.malinghan.marpc.core;

import lombok.Data;

/**
 * RPC 请求协议体，描述一次远程方法调用所需的全部信息。
 *
 * <p>由 Consumer 侧的 {@link com.malinghan.marpc.consumer.MarpcInvocationHandler} 构造，
 * 序列化为 JSON 后通过 HTTP 传输到 Provider。
 */
@Data
public class RpcRequest {
    /** 服务接口的全限定名，用于在 Provider 的 skeleton map 中定位实现类 */
    private String service;
    /** 目标方法名，配合参数个数在实现类上定位具体方法 */
    private String method;
    /** 方法参数列表，JSON 序列化传输，Provider 侧按参数个数匹配方法后直接传入 */
    private Object[] args;
}