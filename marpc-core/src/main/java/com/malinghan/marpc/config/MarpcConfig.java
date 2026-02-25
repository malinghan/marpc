package com.malinghan.marpc.config;

import com.malinghan.marpc.consumer.ConsumerBootstrap;
import com.malinghan.marpc.filter.CacheFilter;
import com.malinghan.marpc.filter.Filter;
import com.malinghan.marpc.filter.MockFilter;
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

import java.util.ArrayList;
import java.util.List;

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

    /** 是否启用 CacheFilter，默认关闭 */
    @Value("${marpc.filter.cache.enabled:false}")
    private boolean cacheEnabled;

    /** 是否启用 MockFilter，默认关闭 */
    @Value("${marpc.filter.mock.enabled:false}")
    private boolean mockEnabled;

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

    @Bean
    public CacheFilter cacheFilter() {
        return new CacheFilter();
    }

    @Bean
    public MockFilter mockFilter() {
        return new MockFilter();
    }

    /** 组装 Filter 链，按配置开关决定是否启用 */
    @Bean
    public List<Filter> filterChain(CacheFilter cacheFilter, MockFilter mockFilter) {
        List<Filter> chain = new ArrayList<>();
        if (mockEnabled) {
            chain.add(mockFilter);
            log.info("[MarpcConfig] MockFilter 已启用");
        }
        if (cacheEnabled) {
            chain.add(cacheFilter);
            log.info("[MarpcConfig] CacheFilter 已启用");
        }
        return chain;
    }

    @Bean
    public ProviderBootstrap providerBootstrap(ApplicationContext context,
                                               RegistryCenter registryCenter) {
        return new ProviderBootstrap(context, registryCenter, providerInstance);
    }

    @Bean
    public ConsumerBootstrap consumerBootstrap(ApplicationContext context,
                                               RegistryCenter registryCenter,
                                               LoadBalancer loadBalancer,
                                               List<Filter> filterChain,
                                               ProviderBootstrap providerBootstrap) {
        return new ConsumerBootstrap(context, registryCenter, loadBalancer, filterChain);
    }

    @Bean
    public MarpcTransport marpcTransport(ProviderBootstrap providerBootstrap) {
        return new MarpcTransport(providerBootstrap);
    }
}
