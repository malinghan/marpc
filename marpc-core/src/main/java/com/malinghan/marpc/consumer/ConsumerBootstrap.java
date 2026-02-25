package com.malinghan.marpc.consumer;

import com.malinghan.marpc.annotation.MarpcConsumer;
import com.malinghan.marpc.exception.MarpcFrameworkException;
import com.malinghan.marpc.filter.Filter;
import com.malinghan.marpc.loadbalance.LoadBalancer;
import com.malinghan.marpc.registry.RegistryCenter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.malinghan.marpc.exception.MarpcException.ErrorCode.*;

@Slf4j
public class ConsumerBootstrap implements InitializingBean {

    private final ApplicationContext context;
    private final RegistryCenter registryCenter;
    private final LoadBalancer loadBalancer;
    private final List<Filter> filters;
    private final Map<String, List<String>> serviceInstances = new ConcurrentHashMap<>();

    public ConsumerBootstrap(ApplicationContext context, RegistryCenter registryCenter,
                             LoadBalancer loadBalancer, List<Filter> filters) {
        this.context = context;
        this.registryCenter = registryCenter;
        this.loadBalancer = loadBalancer;
        this.filters = filters;
    }

    @Override
    public void afterPropertiesSet() {
        log.info("[ConsumerBootstrap] === 启动阶段：扫描并注入 RPC 代理 ===");
        log.info("[ConsumerBootstrap] 已加载 {} 个 Filter: {}", filters.size(),
                filters.stream().map(f -> f.getClass().getSimpleName()).toList());
        Map<String, Object> beans = context.getBeansOfType(Object.class);
        beans.values().forEach(this::injectConsumers);
        log.info("[ConsumerBootstrap] === 启动完成 ===");
    }

    private void injectConsumers(Object bean) {
        for (Field field : bean.getClass().getDeclaredFields()) {
            if (!field.isAnnotationPresent(MarpcConsumer.class)) continue;
            Class<?> iface = field.getType();
            String service = iface.getCanonicalName();

            List<String> instances = registryCenter.fetchAll(service);
            serviceInstances.put(service, instances);
            log.info("[ConsumerBootstrap] 发现实例: {} -> {}", service, instances);

            registryCenter.subscribe(service, newInstances -> {
                log.info("[ConsumerBootstrap] 实例变更: {} -> {}", service, newInstances);
                serviceInstances.put(service, newInstances);
            });

            field.setAccessible(true);
            try {
                field.set(bean, createProxy(iface));
                log.info("[ConsumerBootstrap] 注入代理: {}", service);
            } catch (IllegalAccessException e) {
                throw new MarpcFrameworkException(CONSUMER_INJECT_FAILED,
                        "inject failed for: " + service, e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T createProxy(Class<T> iface) {
        String service = iface.getCanonicalName();
        return (T) Proxy.newProxyInstance(
                iface.getClassLoader(),
                new Class[]{iface},
                new MarpcInvocationHandler(iface, () -> {
                    List<String> instances = serviceInstances.get(service);
                    if (instances == null || instances.isEmpty()) {
                        throw new MarpcFrameworkException(NO_AVAILABLE_INSTANCE,
                                "no available instance for: " + service);
                    }
                    return loadBalancer.choose(instances);
                }, filters)
        );
    }
}
