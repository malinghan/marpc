package com.malinghan.marpc.provider;

import com.malinghan.marpc.annotation.MarpcProvider;
import com.malinghan.marpc.core.RpcRequest;
import com.malinghan.marpc.core.RpcResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
public class ProviderBootstrap {

    // 优化2：非用户自定义接口的包前缀，注册时过滤
    private static final Set<String> SYSTEM_PACKAGES = Set.of("java.", "javax.", "org.springframework.");

    private final ApplicationContext context;
    private final Map<String, Object> skeleton = new HashMap<>();

    public ProviderBootstrap(ApplicationContext context) {
        this.context = context;
    }

    public void start() {
        System.out.println("[ProviderBootstrap] 开始扫描 @MarpcProvider 注解的 Bean...");
        Map<String, Object> providers = context.getBeansWithAnnotation(MarpcProvider.class);
        System.out.println("[ProviderBootstrap] 找到 " + providers.size() + " 个 Provider Bean");
        providers.values().forEach(bean -> {
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            for (Class<?> iface : targetClass.getInterfaces()) {
                // 优化2：过滤掉系统接口，只注册用户自定义接口
                if (isUserDefinedInterface(iface)) {
                    skeleton.put(iface.getCanonicalName(), bean);
                    System.out.println("[ProviderBootstrap] 注册服务: " + iface.getCanonicalName());
                    log.info("marpc provider registered: {}", iface.getCanonicalName());
                } else {
                    System.out.println("[ProviderBootstrap] 过滤系统接口: " + iface.getCanonicalName());
                }
            }
        });
        System.out.println("[ProviderBootstrap] 服务注册完成，共 " + skeleton.size() + " 个服务");
    }

    private boolean isUserDefinedInterface(Class<?> iface) {
        String pkg = iface.getName();
        return SYSTEM_PACKAGES.stream().noneMatch(pkg::startsWith);
    }

    /**
     * 处理一次 RPC 请求：查找服务实现 -> 定位方法 -> 反射调用 -> 包装响应。
     * 任何异常都被捕获并转为 error 响应，不向传输层抛出。
     */
    public RpcResponse invoke(RpcRequest request) {
        System.out.println("[ProviderBootstrap] 收到请求: service=" + request.getService()
                + ", method=" + request.getMethod()
                + ", args=" + java.util.Arrays.toString(request.getArgs()));
        System.out.println("[ProviderBootstrap] 当前 skeleton keys: " + skeleton.keySet());
        Object bean = skeleton.get(request.getService());
        if (bean == null) {
            System.out.println("[ProviderBootstrap] 服务未找到: " + request.getService());
            return RpcResponse.error("service not found: " + request.getService());
        }
        try {
            Method method = findMethod(bean.getClass(), request.getMethod(), request.getArgs());
            System.out.println("[ProviderBootstrap] 执行方法: " + method);
            Object result = ReflectionUtils.invokeMethod(method, bean, request.getArgs());
            System.out.println("[ProviderBootstrap] 执行结果: " + result);
            return RpcResponse.ok(result);
        } catch (Exception e) {
            // 优化3：封装异常类名和消息，透传给 Consumer
            log.error("marpc invoke error", e);
            String errorMsg = e.getClass().getName() + ": " +
                (e.getMessage() != null ? e.getMessage() : "null");
            System.out.println("[ProviderBootstrap] 执行异常: " + errorMsg);
            return RpcResponse.error(errorMsg);
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