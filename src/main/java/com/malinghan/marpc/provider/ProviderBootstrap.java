package com.malinghan.marpc.provider;

import com.alibaba.fastjson2.JSON;
import com.malinghan.marpc.annotation.MarpcProvider;
import com.malinghan.marpc.core.RpcRequest;
import com.malinghan.marpc.core.RpcResponse;
import com.malinghan.marpc.registry.RegistryCenter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class ProviderBootstrap {

    private static final Set<String> SYSTEM_PACKAGES = Set.of("java.", "javax.", "org.springframework.");

    private final ApplicationContext context;
    private final RegistryCenter registryCenter;
    private final String instance; // host:port
    private final Map<String, Object> skeleton = new HashMap<>();

    public ProviderBootstrap(ApplicationContext context, RegistryCenter registryCenter, String instance) {
        this.context = context;
        this.registryCenter = registryCenter;
        this.instance = instance;
    }

    public void start() {
        Map<String, Object> providers = context.getBeansWithAnnotation(MarpcProvider.class);
        log.info("[ProviderBootstrap] 找到 {} 个 Provider Bean", providers.size());
        providers.values().forEach(bean -> {
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            for (Class<?> iface : targetClass.getInterfaces()) {
                if (isUserDefinedInterface(iface)) {
                    String service = iface.getCanonicalName();
                    skeleton.put(service, bean);
                    registryCenter.register(service, instance);
                    log.info("[ProviderBootstrap] 注册服务: {} -> {}", service, instance);
                }
            }
        });
        log.info("[ProviderBootstrap] 服务注册完成，共 {} 个服务", skeleton.size());
    }

    public void stop() {
        skeleton.keySet().forEach(service -> {
            registryCenter.unregister(service, instance);
            log.info("[ProviderBootstrap] 注销服务: {} -> {}", service, instance);
        });
        registryCenter.stop();
    }

    private boolean isUserDefinedInterface(Class<?> iface) {
        String pkg = iface.getName();
        return SYSTEM_PACKAGES.stream().noneMatch(pkg::startsWith);
    }

    public RpcResponse invoke(RpcRequest request) {
        Object bean = skeleton.get(request.getService());
        if (bean == null) {
            return RpcResponse.error("service not found: " + request.getService());
        }
        try {
            Method method = findMethod(AopUtils.getTargetClass(bean), request.getMethodSign(), request.getMethod());
            Object[] typedArgs = convertArgs(method, request.getArgs());
            Object result = ReflectionUtils.invokeMethod(method, bean, typedArgs);
            return RpcResponse.ok(result);
        } catch (Exception e) {
            log.error("[ProviderBootstrap] invoke error", e);
            String msg = e.getClass().getName() + ": " + (e.getMessage() != null ? e.getMessage() : "null");
            return RpcResponse.error(msg);
        }
    }

    private Method findMethod(Class<?> clazz, String methodSign, String methodName) {
        if (methodSign != null && !methodSign.isEmpty()) {
            for (Method m : clazz.getMethods()) {
                if (buildSign(m).equals(methodSign)) return m;
            }
        }
        int argCount = methodSign != null && methodSign.contains("@")
                ? Integer.parseInt(methodSign.split("@")[1].split("_")[0])
                : 0;
        for (Method m : clazz.getMethods()) {
            if (m.getName().equals(methodName) && m.getParameterCount() == argCount) return m;
        }
        throw new RuntimeException("method not found: " + methodSign);
    }

    private String buildSign(Method m) {
        Class<?>[] params = m.getParameterTypes();
        if (params.length == 0) return m.getName() + "@0";
        String types = Arrays.stream(params)
                .map(Class::getCanonicalName)
                .collect(Collectors.joining("_"));
        return m.getName() + "@" + params.length + "_" + types;
    }

    private Object[] convertArgs(Method method, Object[] args) {
        if (args == null || args.length == 0) return args;
        Class<?>[] paramTypes = method.getParameterTypes();
        Object[] result = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            result[i] = JSON.to(paramTypes[i], args[i]);
        }
        return result;
    }
}
