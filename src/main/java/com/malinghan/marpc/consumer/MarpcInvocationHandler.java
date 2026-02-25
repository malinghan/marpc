package com.malinghan.marpc.consumer;

import com.alibaba.fastjson2.JSON;
import com.malinghan.marpc.core.RpcRequest;
import com.malinghan.marpc.core.RpcResponse;
import okhttp3.*;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Consumer 侧 JDK 动态代理的拦截器。
 *
 * <p>每个 RPC 接口对应一个实例，由 {@link ConsumerBootstrap} 在启动时创建并注入。
 * 当业务代码调用接口方法时，JDK 代理将调用路由到此处的 {@link #invoke} 方法，
 * 由它负责将本地方法调用转换为一次远程 HTTP 请求。
 */
public class MarpcInvocationHandler implements InvocationHandler {

    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");
    /** 被代理的服务接口，用于填充 RpcRequest.service 字段 */
    private final Class<?> service;
    /** Provider 的基础 URL，例如 http://localhost:8080 */
    private final String providerUrl;
    private final OkHttpClient client = new OkHttpClient();

    public MarpcInvocationHandler(Class<?> service, String providerUrl) {
        this.service = service;
        this.providerUrl = providerUrl;
    }

    /**
     * 拦截所有接口方法调用。
     *
     * <p>来自 {@link Object} 的方法（equals / hashCode / toString）直接在本地执行，
     * 避免将这些基础方法误发到远端。其余方法封装为 {@link RpcRequest} 并通过 HTTP 发送。
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // Object 基类方法不走网络，直接本地执行
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(this, args);
        }

        // 构造 RPC 请求：接口全限定名 + 方法名 + 参数列表
        RpcRequest request = new RpcRequest();
        request.setService(service.getCanonicalName());
        request.setMethod(method.getName());
        request.setArgs(args);

        RpcResponse response = post(request);
        // Provider 返回业务异常时，转为 RuntimeException 抛给调用方
        if (!response.isStatus()) {
            throw new RuntimeException(response.getErrorMessage());
        }
        // fastjson2 根据方法声明的返回类型将 data 反序列化为正确的 Java 对象
        return JSON.to(method.getReturnType(), response.getData());
    }

    /**
     * 将 {@link RpcRequest} 序列化为 JSON 并 POST 到 Provider 的 /marpc 端点。
     *
     * @return 反序列化后的 {@link RpcResponse}
     */
    private RpcResponse post(RpcRequest request) throws Exception {
        String body = JSON.toJSONString(request);
        Request httpRequest = new Request.Builder()
                .url(providerUrl + "/marpc")
                .post(RequestBody.create(body, JSON_TYPE))
                .build();
        // try-with-resources 确保响应体被关闭，避免连接泄漏
        try (Response resp = client.newCall(httpRequest).execute()) {
            String json = resp.body().string();
            return JSON.parseObject(json, RpcResponse.class);
        }
    }
}
