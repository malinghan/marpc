package com.malinghan.marpc.config;

import com.malinghan.marpc.circuitbreaker.CircuitBreaker;
import com.malinghan.marpc.circuitbreaker.CircuitBreakerConfig;
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
import com.malinghan.marpc.retry.RetryPolicy;
import com.malinghan.marpc.router.GrayRouter;
import com.malinghan.marpc.router.Router;
import com.malinghan.marpc.transport.MarpcTransport;
import com.malinghan.marpc.transport.OkHttpTransport;
import com.malinghan.marpc.transport.RpcTransport;
import com.malinghan.marpc.transport.netty.NettyRpcClient;
import com.malinghan.marpc.transport.netty.NettyRpcServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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

    @Value("${marpc.filter.cache.enabled:false}")
    private boolean cacheEnabled;

    @Value("${marpc.filter.mock.enabled:false}")
    private boolean mockEnabled;

    @Value("${marpc.retry.maxRetries:2}")
    private int maxRetries;

    @Value("${marpc.retry.timeout:3000}")
    private int timeout;

    @Value("${marpc.retry.switchInstanceOnRetry:true}")
    private boolean switchInstanceOnRetry;

    @Value("${marpc.circuitbreaker.enabled:false}")
    private boolean circuitBreakerEnabled;

    @Value("${marpc.circuitbreaker.faultLimit:5}")
    private int faultLimit;

    @Value("${marpc.circuitbreaker.halfOpenInitialDelay:10000}")
    private long halfOpenInitialDelay;

    @Value("${marpc.circuitbreaker.halfOpenDelay:5000}")
    private long halfOpenDelay;

    @Value("${marpc.circuitbreaker.windowSize:10}")
    private int windowSize;

    @Value("${marpc.router.gray.enabled:false}")
    private boolean grayRouterEnabled;

    @Value("${marpc.router.gray.ratio:0}")
    private int grayRatio;

    @Value("${marpc.transport:okhttp}")
    private String transportType;

    @Value("${marpc.netty.port:9090}")
    private int nettyPort;

    @Value("${marpc.netty.server.enabled:false}")
    private boolean nettyServerEnabled;

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
    public GrayRouter grayRouter() {
        return new GrayRouter(grayRatio);
    }

    @Bean
    public List<Router> routerChain(GrayRouter grayRouter) {
        List<Router> chain = new ArrayList<>();
        if (grayRouterEnabled) {
            chain.add(grayRouter);
            log.info("[MarpcConfig] GrayRouter 已启用，灰度比例: {}%", grayRatio);
        }
        return chain;
    }

    @Bean
    public RetryPolicy retryPolicy() {
        RetryPolicy policy = new RetryPolicy();
        policy.setMaxRetries(maxRetries);
        policy.setTimeout(timeout);
        policy.setSwitchInstanceOnRetry(switchInstanceOnRetry);
        return policy;
    }

    @Bean
    public CircuitBreaker circuitBreaker() {
        CircuitBreakerConfig config = new CircuitBreakerConfig();
        config.setEnabled(circuitBreakerEnabled);
        config.setFaultLimit(faultLimit);
        config.setHalfOpenInitialDelay(halfOpenInitialDelay);
        config.setHalfOpenDelay(halfOpenDelay);
        config.setWindowSize(windowSize);
        return new CircuitBreaker(config);
    }

    @Bean
    public ProviderBootstrap providerBootstrap(ApplicationContext context,
                                               RegistryCenter registryCenter) {
        return new ProviderBootstrap(context, registryCenter, providerInstance);
    }

    @Bean
    public RpcTransport rpcTransport() {
        if ("netty".equalsIgnoreCase(transportType)) {
            log.info("[MarpcConfig] 使用 Netty 传输");
            return new NettyRpcClient(timeout, nettyPort);
        }
        log.info("[MarpcConfig] 使用 OkHttp 传输");
        return new OkHttpTransport(timeout);
    }

    @Bean
    @ConditionalOnProperty(name = "marpc.netty.server.enabled", havingValue = "true")
    public NettyRpcServer nettyRpcServer(ProviderBootstrap providerBootstrap) {
        return new NettyRpcServer(providerBootstrap, nettyPort);
    }

    @Bean
    public ConsumerBootstrap consumerBootstrap(ApplicationContext context,
                                               RegistryCenter registryCenter,
                                               LoadBalancer loadBalancer,
                                               List<Filter> filterChain,
                                               RetryPolicy retryPolicy,
                                               CircuitBreaker circuitBreaker,
                                               List<Router> routerChain,
                                               RpcTransport rpcTransport,
                                               ProviderBootstrap providerBootstrap) {
        return new ConsumerBootstrap(context, registryCenter, loadBalancer, filterChain,
                retryPolicy, circuitBreaker, routerChain, rpcTransport);
    }

    @Bean
    public MarpcTransport marpcTransport(ProviderBootstrap providerBootstrap) {
        return new MarpcTransport(providerBootstrap);
    }
}
