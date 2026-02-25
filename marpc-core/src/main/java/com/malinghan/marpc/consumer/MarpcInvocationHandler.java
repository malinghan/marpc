package com.malinghan.marpc.consumer;

import com.alibaba.fastjson2.JSON;
import com.malinghan.marpc.circuitbreaker.CircuitBreaker;
import com.malinghan.marpc.context.RpcContext;
import com.malinghan.marpc.core.RpcRequest;
import com.malinghan.marpc.core.RpcResponse;
import com.malinghan.marpc.exception.MarpcBizException;
import com.malinghan.marpc.exception.MarpcNetworkException;
import com.malinghan.marpc.filter.Filter;
import com.malinghan.marpc.retry.RetryPolicy;
import com.malinghan.marpc.transport.RpcTransport;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.malinghan.marpc.exception.MarpcException.ErrorCode.*;

@Slf4j
public class MarpcInvocationHandler implements InvocationHandler {

    private static final Set<String> SYSTEM_PACKAGES = Set.of("java.", "javax.", "org.springframework.");

    private final Class<?> service;
    private final Supplier<String> instanceSupplier;
    private final List<Filter> filters;
    private final RetryPolicy retryPolicy;
    private final CircuitBreaker circuitBreaker;
    private final RpcTransport transport;

    public MarpcInvocationHandler(Class<?> service, Supplier<String> instanceSupplier,
                                   List<Filter> filters, RetryPolicy retryPolicy,
                                   CircuitBreaker circuitBreaker, RpcTransport transport) {
        this.service = service;
        this.instanceSupplier = instanceSupplier;
        this.retryPolicy = retryPolicy;
        this.circuitBreaker = circuitBreaker;
        this.transport = transport;
        this.filters = filters.stream()
                .sorted((a, b) -> Integer.compare(a.order(), b.order()))
                .collect(Collectors.toList());
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

        // 填充隐式上下文参数
        Map<String, String> ctx = RpcContext.getAll();
        if (!ctx.isEmpty()) {
            request.setContext(ctx);
        }

        try {
            // preFilter：任意 Filter 返回非 null 则短路
            for (Filter filter : filters) {
                RpcResponse shortCircuit = filter.preFilter(request);
                if (shortCircuit != null) {
                    return convertResponse(method, shortCircuit);
                }
            }

            // 熔断器检查
            circuitBreaker.preCall();

            // 发起远程调用（带重试）
            RpcResponse response = invokeWithRetry(request);

            // postFilter：逆序执行
            List<Filter> reversed = new ArrayList<>(filters);
            Collections.reverse(reversed);
            for (Filter filter : reversed) {
                filter.postFilter(request, response);
            }

            if (!response.isStatus()) {
                throw new MarpcBizException(SERVICE_NOT_FOUND, response.getErrorMessage());
            }

            return convertResponse(method, response);
        } finally {
            RpcContext.clear();
        }
    }

    private RpcResponse invokeWithRetry(RpcRequest request) {
        Set<String> triedInstances = new HashSet<>();
        int attempts = 0;
        int maxAttempts = 1 + retryPolicy.getMaxRetries();
        Throwable lastError = null;

        while (attempts < maxAttempts) {
            attempts++;
            String instance = selectInstance(triedInstances);
            triedInstances.add(instance);

            try {
                RpcResponse response = transport.send(instance, request);
                if (response.isStatus()) {
                    circuitBreaker.onSuccess();
                    if (attempts > 1) {
                        log.info("[Retry] 第 {} 次调用成功: {}", attempts, instance);
                    }
                    return response;
                }
                return response;
            } catch (MarpcNetworkException e) {
                lastError = e;
                circuitBreaker.onFailure();
                if (attempts < maxAttempts) {
                    log.warn("[Retry] 第 {} 次调用失败: {}, 原因: {}", attempts, instance, e.getMessage());
                } else {
                    log.error("[Retry] 重试 {} 次后仍失败", retryPolicy.getMaxRetries());
                }
            }
        }

        throw new MarpcNetworkException(NETWORK_ERROR,
                "调用失败，已重试 " + retryPolicy.getMaxRetries() + " 次", lastError);
    }

    private String selectInstance(Set<String> triedInstances) {
        if (!retryPolicy.isSwitchInstanceOnRetry() || triedInstances.isEmpty()) {
            return instanceSupplier.get();
        }
        for (int i = 0; i < 10; i++) {
            String instance = instanceSupplier.get();
            if (!triedInstances.contains(instance)) {
                return instance;
            }
        }
        return instanceSupplier.get();
    }

    private Object convertResponse(Method method, RpcResponse response) {
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
}
