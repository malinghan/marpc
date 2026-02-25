package com.malinghan.marpc.consumer;

import com.alibaba.fastjson2.JSON;
import com.malinghan.marpc.core.RpcRequest;
import com.malinghan.marpc.core.RpcResponse;
import okhttp3.*;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Set;

public class MarpcInvocationHandler implements InvocationHandler {

    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");
    // 非用户自定义接口的包前缀，来自这些包的方法直接本地执行
    private static final Set<String> SYSTEM_PACKAGES = Set.of("java.", "javax.", "org.springframework.");

    private final Class<?> service;
    private final String providerUrl;
    private final OkHttpClient client = new OkHttpClient();

    public MarpcInvocationHandler(Class<?> service, String providerUrl) {
        this.service = service;
        this.providerUrl = providerUrl;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 优化2：非用户自定义接口的方法（Object、Spring 等）直接本地执行
        if (isSystemMethod(method)) {
            return method.invoke(this, args);
        }

        RpcRequest request = new RpcRequest();
        request.setService(service.getCanonicalName());
        request.setMethod(method.getName());
        request.setArgs(args);

        RpcResponse response = post(request);
        if (!response.isStatus()) {
            // 优化3：将服务端封装的异常信息透传给调用方
            throw new RuntimeException(response.getErrorMessage());
        }

        // 优化1：基本类型及其包装类直接强转，跳过 JSON 转换
        Class<?> returnType = method.getReturnType();
        if (isPrimitive(returnType)) {
            return castPrimitive(returnType, response.getData());
        }
        return JSON.to(returnType, response.getData());
    }

    private boolean isSystemMethod(Method method) {
        String pkg = method.getDeclaringClass().getName();
        return SYSTEM_PACKAGES.stream().anyMatch(pkg::startsWith);
    }

    private boolean isPrimitive(Class<?> type) {
        return type.isPrimitive()
                || type == Integer.class || type == Long.class || type == Double.class
                || type == Float.class || type == Boolean.class || type == Short.class
                || type == Byte.class || type == Character.class;
    }

    private Object castPrimitive(Class<?> type, Object data) {
        if (data == null) return null;
        String s = data.toString();
        if (type == int.class || type == Integer.class) return Integer.parseInt(s);
        if (type == long.class || type == Long.class) return Long.parseLong(s);
        if (type == double.class || type == Double.class) return Double.parseDouble(s);
        if (type == float.class || type == Float.class) return Float.parseFloat(s);
        if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(s);
        if (type == short.class || type == Short.class) return Short.parseShort(s);
        if (type == byte.class || type == Byte.class) return Byte.parseByte(s);
        if (type == char.class || type == Character.class) return s.charAt(0);
        return data;
    }

    private RpcResponse post(RpcRequest request) throws Exception {
        String body = JSON.toJSONString(request);
        Request httpRequest = new Request.Builder()
                .url(providerUrl + "/marpc")
                .post(RequestBody.create(body, JSON_TYPE))
                .build();
        try (Response resp = client.newCall(httpRequest).execute()) {
            String json = resp.body().string();
            return JSON.parseObject(json, RpcResponse.class);
        }
    }
}
