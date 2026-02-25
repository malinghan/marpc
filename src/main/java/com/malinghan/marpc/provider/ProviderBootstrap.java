package com.malinghan.marpc.provider;

import com.malinghan.marpc.annotation.MarpcProvider;
import com.malinghan.marpc.core.RpcRequest;
import com.malinghan.marpc.core.RpcResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Provider 侧启动引导类，负责服务注册与请求分发。
 *
 * <p>启动时扫描所有标注 {@link MarpcProvider} 的 Bean，将其实现的接口以全限定名为 key
 * 存入 skeleton map。收到 RPC 请求时，通过 skeleton map 查找实现类并用反射执行目标方法。
 */
@Slf4j
public class ProviderBootstrap {

    private final ApplicationContext context;
    // 接口全限定名 -> 实现类实例，是服务查找的核心数据结构
    private final Map<String, Object> skeleton = new HashMap<>();

    public ProviderBootstrap(ApplicationContext context) {
        this.context = context;
    }

    /**
     * 扫描所有 {@link MarpcProvider} Bean，将其实现的接口注册到 skeleton map。
     * 由 {@link com.malinghan.marpc.config.MarpcConfig} 在 Bean 初始化时调用。
     */
    public void start() {
        Map<String, Object> providers = context.getBeansWithAnnotation(MarpcProvider.class);
        providers.values().forEach(bean -> {
            // 一个实现类可能实现多个接口，逐一注册
            for (Class<?> iface : bean.getClass().getInterfaces()) {
                skeleton.put(iface.getCanonicalName(), bean);
                log.info("marpc provider registered: {}", iface.getCanonicalName());
            }
        });
    }

    /**
     * 处理一次 RPC 请求：查找服务实现 -> 定位方法 -> 反射调用 -> 包装响应。
     * 任何异常都被捕获并转为 error 响应，不向传输层抛出。
     */
    public RpcResponse invoke(RpcRequest request) {
        Object bean = skeleton.get(request.getService());
        if (bean == null) {
            return RpcResponse.error("service not found: " + request.getService());
        }
        try {
            Method method = findMethod(bean.getClass(), request.getMethod(), request.getArgs());
            Object result = ReflectionUtils.invokeMethod(method, bean, request.getArgs());
            return RpcResponse.ok(result);
        } catch (Exception e) {
            log.error("marpc invoke error", e);
            return RpcResponse.error(e.getMessage());
        }
    }

    /**
     * 按方法名和参数个数在实现类上定位方法。
     * v1.0 不支持同名同参数数量的重载，遇到第一个匹配即返回。
     */
    private Method findMethod(Class<?> clazz, String methodName, Object[] args) {
        int argCount = args == null ? 0 : args.length;
        for (Method m : clazz.getMethods()) {
            if (m.getName().equals(methodName) && m.getParameterCount() == argCount) {
                return m;
            }
        }
        throw new RuntimeException("method not found: " + methodName);
    }
}