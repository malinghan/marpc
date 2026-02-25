package com.malinghan.marpc.consumer;

import com.malinghan.marpc.annotation.MarpcConsumer;
import com.malinghan.marpc.loadbalance.LoadBalancer;
import com.malinghan.marpc.registry.RegistryCenter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ConsumerBootstrap {

    private final ApplicationContext context;
    private final RegistryCenter registryCenter;
    private final LoadBalancer loadBalancer;
    // service -> 当前可用实例列表（动态刷新）
    private final Map<String, List<String>> serviceInstances = new ConcurrentHashMap<>();

    public ConsumerBootstrap(ApplicationContext context, RegistryCenter registryCenter, LoadBalancer loadBalancer) {
        this.context = context;
        this.registryCenter = registryCenter;
        this.loadBalancer = loadBalancer;
    }

    public void start() {
        Map<String, Object> beans = context.getBeansOfType(Object.class);
        beans.values().forEach(this::injectConsumers);
    }

    private void injectConsumers(Object bean) {
        for (Field field : bean.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(MarpcConsumer.class)) {
                Class<?> iface = field.getType();
                String service = iface.getCanonicalName();

                // 拉取初始实例列表并订阅变更
                List<String> instances = registryCenter.fetchAll(service);
                serviceInstances.put(service, instances);
                registryCenter.subscribe(service, newInstances -> {
                    log.info("[ConsumerBootstrap] 服务实例变更: {} -> {}", service, newInstances);
                    serviceInstances.put(service, newInstances);
                });

                field.setAccessible(true);
                Object proxy = createProxy(iface);
                try {
                    field.set(bean, proxy);
                    log.info("[ConsumerBootstrap] 注入代理: {}", service);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
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
                        throw new RuntimeException("no available instance for: " + service);
                    }
                    return loadBalancer.choose(instances);
                })
        );
    }
}
