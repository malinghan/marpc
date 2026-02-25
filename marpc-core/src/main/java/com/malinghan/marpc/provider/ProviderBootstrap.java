package com.malinghan.marpc.provider;

import com.alibaba.fastjson2.JSON;
import com.malinghan.marpc.annotation.MarpcProvider;
import com.malinghan.marpc.core.RpcRequest;
import com.malinghan.marpc.core.RpcResponse;
import com.malinghan.marpc.exception.MarpcBizException;
import com.malinghan.marpc.registry.RegistryCenter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.malinghan.marpc.exception.MarpcException.ErrorCode.*;

@Slf4j
public class ProviderBootstrap implements InitializingBean, DisposableBean {

    private static final Set<String> SYSTEM_PACKAGES = Set.of("java.", "javax.", "org.springframework.");

    private final ApplicationContext context;
    private final RegistryCenter registryCenter;
    private final String instance;
    private final Map<String, Object> skeleton = new HashMap<>();

    public ProviderBootstrap(ApplicationContext context, RegistryCenter registryCenter, String instance) {
        this.context = context;
        this.registryCenter = registryCenter;
        this.instance = instance;
    }

    /** Spring InitializingBean 回调，所有 Bean 就绪后自动执行 */
    @Override
    public void afterPropertiesSet() {
        log.info("[ProviderBootstrap] === 启动阶段：扫描并注册服务 ===");
        Map<String, Object> providers = context.getBeansWithAnnotation(MarpcProvider.class);
        log.info("[ProviderBootstrap] 发现 {} 个 @MarpcProvider Bean", providers.size());
        providers.values().forEach(bean -> {
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            for (Class<?> iface : targetClass.getInterfaces()) {
                if (isUserDefinedInterface(iface)) {
                    String service = iface.getCanonicalName();
                    skeleton.put(service, bean);
                    registryCenter.register(service, instance);
                    log.info("[ProviderBootstrap] 注册: {} -> {}", service, instance);
                }
            }
        });
        log.info("[ProviderBootstrap] === 启动完成，共 {} 个服务 ===", skeleton.size());
    }

    /** Spring DisposableBean 回调，容器关闭时自动执行 */
    @Override
    public void destroy() {
        log.info("[ProviderBootstrap] === 关闭阶段：注销服务 ===");
        skeleton.keySet().forEach(service -> {
            registryCenter.unregister(service, instance);
            log.info("[ProviderBootstrap] 注销: {} -> {}", service, instance);
        });
        registryCenter.stop();
        log.info("[ProviderBootstrap] === 关闭完成 ===");
    }

    public RpcResponse invoke(RpcRequest request) {
        Object bean = skeleton.get(request.getService());
        if (bean == null) {
            throw new MarpcBizException(SERVICE_NOT_FOUND,
                    "service not found: " + request.getService());
        }
        try {
            Method method = findMethod(AopUtils.getTargetClass(bean),
                    request.getMethodSign(), request.getMethod());
            Object[] typedArgs = convertArgs(method, request.getArgs());
            Object result = ReflectionUtils.invokeMethod(method, bean, typedArgs);
            return RpcResponse.ok(result);
        } catch (MarpcBizException e) {
            return RpcResponse.error(e.getErrorCode() + ": " + e.getMessage());
        } catch (Exception e) {
            log.error("[ProviderBootstrap] invoke error", e);
            return RpcResponse.error(e.getClass().getName() + ": " +
                    (e.getMessage() != null ? e.getMessage() : "null"));
        }
    }

    private boolean isUserDefinedInterface(Class<?> iface) {
        String pkg = iface.getName();
        return SYSTEM_PACKAGES.stream().noneMatch(pkg::startsWith);
    }

    private Method findMethod(Class<?> clazz, String methodSign, String methodName) {
        if (methodSign != null && !methodSign.isEmpty()) {
            for (Method m : clazz.getMethods()) {
                if (buildSign(m).equals(methodSign)) return m;
            }
        }
        int argCount = methodSign != null && methodSign.contains("@")
                ? Integer.parseInt(methodSign.split("@")[1].split("_")[0]) : 0;
        for (Method m : clazz.getMethods()) {
            if (m.getName().equals(methodName) && m.getParameterCount() == argCount) return m;
        }
        throw new MarpcBizException(METHOD_NOT_FOUND, "method not found: " + methodSign);
    }

    private String buildSign(Method m) {
        Class<?>[] params = m.getParameterTypes();
        if (params.length == 0) return m.getName() + "@0";
        String types = Arrays.stream(params).map(Class::getCanonicalName)
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
