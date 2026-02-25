package com.malinghan.marpc.config;

import com.malinghan.marpc.consumer.ConsumerBootstrap;
import com.malinghan.marpc.loadbalance.LoadBalancer;
import com.malinghan.marpc.loadbalance.RandomLoadBalancer;
import com.malinghan.marpc.loadbalance.RoundRobinLoadBalancer;
import com.malinghan.marpc.provider.ProviderBootstrap;
import com.malinghan.marpc.registry.RegistryCenter;
import com.malinghan.marpc.registry.ZkRegistryCenter;
import com.malinghan.marpc.transport.MarpcTransport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class MarpcConfig {

    @Value("${marpc.zk.address:localhost:2181}")
    private String zkAddress;

    @Value("${marpc.app:marpc-app}")
    private String app;

    @Value("${marpc.env:dev}")
    private String env;

    @Value("${marpc.provider.instance:localhost:8080}")
    private String providerInstance;

    @Value("${marpc.loadbalancer:roundrobin}")
    private String lbStrategy;

    @Bean
    public RegistryCenter registryCenter() {
        ZkRegistryCenter rc = new ZkRegistryCenter(zkAddress, app, env);
        rc.start();
        return rc;
    }

    @Bean
    public LoadBalancer loadBalancer() {
        if ("random".equalsIgnoreCase(lbStrategy)) return new RandomLoadBalancer();
        return new RoundRobinLoadBalancer();
    }

    /**
     * ProviderBootstrap 实现 InitializingBean，afterPropertiesSet 会在 Bean 创建后自动调用，
     * 无需 ContextRefreshedEvent 监听器。
     */
    @Bean
    public ProviderBootstrap providerBootstrap(ApplicationContext context,
                                               RegistryCenter registryCenter) {
        return new ProviderBootstrap(context, registryCenter, providerInstance);
    }

    /**
     * ConsumerBootstrap 实现 InitializingBean，afterPropertiesSet 会在 Bean 创建后自动调用。
     * 声明对 ProviderBootstrap 的依赖，确保 Provider 先完成注册。
     */
    @Bean
    public ConsumerBootstrap consumerBootstrap(ApplicationContext context,
                                               RegistryCenter registryCenter,
                                               LoadBalancer loadBalancer,
                                               ProviderBootstrap providerBootstrap) {
        return new ConsumerBootstrap(context, registryCenter, loadBalancer);
    }

    @Bean
    public MarpcTransport marpcTransport(ProviderBootstrap providerBootstrap) {
        return new MarpcTransport(providerBootstrap);
    }
}
