package com.malinghan.marpc.config;

import com.malinghan.marpc.consumer.ConsumerBootstrap;
import com.malinghan.marpc.loadbalance.LoadBalancer;
import com.malinghan.marpc.loadbalance.RoundRobinLoadBalancer;
import com.malinghan.marpc.provider.ProviderBootstrap;
import com.malinghan.marpc.registry.RegistryCenter;
import com.malinghan.marpc.registry.ZkRegistryCenter;
import com.malinghan.marpc.transport.MarpcTransport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;

@Configuration
public class MarpcConfig {

    @Value("${marpc.zk.address:localhost:2181}")
    private String zkAddress;

    @Value("${marpc.app:marpc-app}")
    private String app;

    @Value("${marpc.env:dev}")
    private String env;

    /** 本机对外暴露的 host:port，Consumer 通过注册中心发现此地址 */
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
        if ("random".equalsIgnoreCase(lbStrategy)) {
            return new com.malinghan.marpc.loadbalance.RandomLoadBalancer();
        }
        return new RoundRobinLoadBalancer();
    }

    @Bean
    public ProviderBootstrap providerBootstrap(ApplicationContext context, RegistryCenter registryCenter) {
        return new ProviderBootstrap(context, registryCenter, providerInstance);
    }

    @Bean
    public ConsumerBootstrap consumerBootstrap(ApplicationContext context,
                                               RegistryCenter registryCenter,
                                               LoadBalancer loadBalancer) {
        return new ConsumerBootstrap(context, registryCenter, loadBalancer);
    }

    @Bean
    public ApplicationListener<ContextRefreshedEvent> marpcBootstrapListener(
            ProviderBootstrap providerBootstrap,
            ConsumerBootstrap consumerBootstrap) {
        // 防止父子容器重复触发
        return new ApplicationListener<>() {
            private volatile boolean started = false;

            @Override
            public void onApplicationEvent(ContextRefreshedEvent event) {
                if (!started) {
                    started = true;
                    providerBootstrap.start();
                    consumerBootstrap.start();
                }
            }
        };
    }

    @Bean
    public MarpcTransport marpcTransport(ProviderBootstrap providerBootstrap) {
        return new MarpcTransport(providerBootstrap);
    }
}
