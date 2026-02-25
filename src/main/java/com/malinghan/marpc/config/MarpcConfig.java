package com.malinghan.marpc.config;

import com.malinghan.marpc.consumer.ConsumerBootstrap;
import com.malinghan.marpc.provider.ProviderBootstrap;
import com.malinghan.marpc.transport.MarpcTransport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * marpc 框架的核心 Spring 配置类，由 {@link com.malinghan.marpc.annotation.EnableMarpc} 导入。
 *
 * <p>按顺序装配三个核心 Bean：
 * <ol>
 *   <li>{@link ProviderBootstrap} — 扫描并注册服务提供方</li>
 *   <li>{@link ConsumerBootstrap} — 扫描并注入消费方代理（依赖 ProviderBootstrap 先完成注册）</li>
 *   <li>{@link MarpcTransport} — 暴露 HTTP 端点接收远程调用</li>
 * </ol>
 */
@Configuration
public class MarpcConfig {

    /** Provider 地址，默认本机 8080，可通过 marpc.provider.url 覆盖 */
    @Value("${marpc.provider.url:http://localhost:8080}")
    private String providerUrl;

    @Bean
    public ProviderBootstrap providerBootstrap(ApplicationContext context) {
        return new ProviderBootstrap(context);
    }

    /**
     * ConsumerBootstrap 声明对 ProviderBootstrap 的依赖，确保 Provider 先完成服务注册，
     * 再执行 Consumer 的代理注入（同进程场景下调用不会在注册前发出）。
     */
    @Bean
    public ConsumerBootstrap consumerBootstrap(ApplicationContext context, ProviderBootstrap providerBootstrap) {
        return new ConsumerBootstrap(context, providerUrl);
    }

    /**
     * 监听 ContextRefreshedEvent，在所有 Bean 注册完成后再启动 Provider 和 Consumer。
     * 这样可以确保 @MarpcProvider 和 @MarpcConsumer 注解的 Bean 都已经被扫描注册。
     */
    @Bean
    public ApplicationListener<ContextRefreshedEvent> marpcBootstrapListener(
            ProviderBootstrap providerBootstrap,
            ConsumerBootstrap consumerBootstrap) {
        return event -> {
            providerBootstrap.start();
            consumerBootstrap.start();
        };
    }

    @Bean
    public MarpcTransport marpcTransport(ProviderBootstrap providerBootstrap) {
        return new MarpcTransport(providerBootstrap);
    }
}
