package com.malinghan.marpc.consumer;

import com.alibaba.fastjson2.JSON;
import com.malinghan.marpc.core.RpcRequest;
import com.malinghan.marpc.core.RpcResponse;
import com.malinghan.marpc.exception.MarpcBizException;
import com.malinghan.marpc.exception.MarpcNetworkException;
import okhttp3.*;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.malinghan.marpc.exception.MarpcException.ErrorCode.*;

public class MarpcInvocationHandler implements InvocationHandler {

    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");
    private static final Set<String> SYSTEM_PACKAGES = Set.of("java.", "javax.", "org.springframework.");

    private final Class<?> service;
    private final Supplier<String> instanceSupplier;
    private final OkHttpClient client = new OkHttpClient();

    public MarpcInvocationHandler(Class<?> service, Supplier<String> instanceSupplier) {
        this.service = service;
        this.instanceSupplier = instanceSupplier;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (isSystemMethod(method)) {
            return method.invoke(this, args);
        }

        RpcRequest request = new RpcRequest();
        request.setService(service.getCanonicalName());
        request.setMethod(method.getName());
        request.setMethodSign(buildMethodSign(method));
        request.setArgs(args);

        String instance = instanceSupplier.get();
        RpcResponse response = post(instance, request);

        if (!response.isStatus()) {
            throw new MarpcBizException(SERVICE_NOT_FOUND, response.getErrorMessage());
        }

        Class<?> returnType = method.getReturnType();
        if (returnType == void.class) return null;
        if (isPrimitive(returnType)) return castPrimitive(returnType, response.getData());
        return JSON.to(returnType, response.getData());
    }

    private String buildMethodSign(Method method) {
        Class<?>[] params = method.getParameterTypes();
        if (params.length == 0) return method.getName() + "@0";
        String types = Arrays.stream(params).map(Class::getCanonicalName)
                .collect(Collectors.joining("_"));
        return method.getName() + "@" + params.length + "_" + types;
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

    private RpcResponse post(String instance, RpcRequest request) {
        try {
            String url = "http://" + instance + "/marpc";
            String body = JSON.toJSONString(request);
            Request httpRequest = new Request.Builder()
                    .url(url).post(RequestBody.create(body, JSON_TYPE)).build();
            try (Response resp = client.newCall(httpRequest).execute()) {
                String json = resp.body().string();
                return JSON.parseObject(json, RpcResponse.class);
            }
        } catch (Exception e) {
            throw new MarpcNetworkException(NETWORK_ERROR,
                    "call failed: " + instance, e);
        }
    }
}
