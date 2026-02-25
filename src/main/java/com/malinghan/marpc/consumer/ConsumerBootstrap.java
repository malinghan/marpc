package com.malinghan.marpc.consumer;

import com.malinghan.marpc.annotation.MarpcConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Map;

/**
 * Consumer 侧启动引导类，负责扫描并注入 RPC 代理。
 *
 * <p>启动时遍历容器中所有 Bean 的字段，对标注了 {@link MarpcConsumer} 的字段
 * 创建 JDK 动态代理并注入，业务代码调用接口方法时会透明地发起远程调用。
 */
@Slf4j
public class ConsumerBootstrap {

    private final ApplicationContext context;
    /** Provider 的基础 URL，从配置项 marpc.provider.url 读取 */
    private final String providerUrl;

    public ConsumerBootstrap(ApplicationContext context, String providerUrl) {
        this.context = context;
        this.providerUrl = providerUrl;
    }

    /**
     * 遍历容器所有 Bean，为标注 {@link MarpcConsumer} 的字段注入代理。
     * 由 {@link com.malinghan.marpc.config.MarpcConfig} 在 Bean 初始化时调用。
     */
    public void start() {
        Map<String, Object> beans = context.getBeansOfType(Object.class);
        beans.values().forEach(this::injectConsumers);
    }

    /** 检查单个 Bean 的所有字段，发现 {@link MarpcConsumer} 注解则注入代理 */
    private void injectConsumers(Object bean) {
        for (Field field : bean.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(MarpcConsumer.class)) {
                field.setAccessible(true);
                Object proxy = createProxy(field.getType());
                try {
                    field.set(bean, proxy);
                    log.info("marpc consumer injected: {}", field.getType().getCanonicalName());
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /** 为指定接口创建 JDK 动态代理，实际调用由 {@link MarpcInvocationHandler} 处理 */
    @SuppressWarnings("unchecked")
    private <T> T createProxy(Class<T> iface) {
        return (T) Proxy.newProxyInstance(
                iface.getClassLoader(),
                new Class[]{iface},
                new MarpcInvocationHandler(iface, providerUrl)
        );
    }
}
