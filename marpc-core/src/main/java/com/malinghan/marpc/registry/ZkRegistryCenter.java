package com.malinghan.marpc.registry;

import com.malinghan.marpc.exception.MarpcFrameworkException;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.malinghan.marpc.exception.MarpcException.ErrorCode.*;

@Slf4j
public class ZkRegistryCenter implements RegistryCenter {

    private final String zkAddress;
    private final String app;
    private final String env;
    private CuratorFramework client;
    private final Map<String, TreeCache> caches = new ConcurrentHashMap<>();

    public ZkRegistryCenter(String zkAddress, String app, String env) {
        this.zkAddress = zkAddress;
        this.app = app;
        this.env = env;
    }

    @Override
    public void start() {
        client = CuratorFrameworkFactory.builder()
                .connectString(zkAddress)
                .sessionTimeoutMs(30000)
                .connectionTimeoutMs(5000)
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .build();
        client.start();
        log.info("[ZkRegistryCenter] 已连接 Zookeeper: {}", zkAddress);
    }

    @Override
    public void stop() {
        caches.values().forEach(cache -> {
            try { cache.close(); } catch (Exception e) {
                log.error("[ZkRegistryCenter] 关闭 TreeCache 失败", e);
            }
        });
        if (client != null) client.close();
        log.info("[ZkRegistryCenter] 已关闭");
    }

    @Override
    public void register(String service, String instance) {
        try {
            String path = buildPath(service, instance);
            if (client.checkExists().forPath(path) == null) {
                client.create().creatingParentsIfNeeded()
                        .withMode(CreateMode.EPHEMERAL).forPath(path);
                log.info("[ZkRegistryCenter] 注册: {}", path);
            }
        } catch (Exception e) {
            throw new MarpcFrameworkException(PROVIDER_REGISTER_FAILED,
                    "register failed: " + service, e);
        }
    }

    @Override
    public void unregister(String service, String instance) {
        try {
            String path = buildPath(service, instance);
            if (client.checkExists().forPath(path) != null) {
                client.delete().forPath(path);
                log.info("[ZkRegistryCenter] 注销: {}", path);
            }
        } catch (Exception e) {
            log.error("[ZkRegistryCenter] 注销失败: {}", service, e);
        }
    }

    @Override
    public List<String> fetchAll(String service) {
        try {
            String parent = buildServicePath(service);
            if (client.checkExists().forPath(parent) == null) return List.of();
            return client.getChildren().forPath(parent).stream()
                    .map(child -> child.replace("_", ":"))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new MarpcFrameworkException(PROVIDER_REGISTER_FAILED,
                    "fetchAll failed: " + service, e);
        }
    }

    @Override
    public void subscribe(String service, ChangeListener listener) {
        try {
            String parent = buildServicePath(service);
            TreeCache cache = TreeCache.newBuilder(client, parent).build();
            cache.getListenable().addListener((c, event) -> {
                log.info("[ZkRegistryCenter] 服务变更: {}", service);
                listener.onChange(fetchAll(service));
            });
            cache.start();
            caches.put(service, cache);
            log.info("[ZkRegistryCenter] 订阅: {}", service);
        } catch (Exception e) {
            throw new MarpcFrameworkException(PROVIDER_REGISTER_FAILED,
                    "subscribe failed: " + service, e);
        }
    }

    private String buildServicePath(String service) {
        return "/" + app + "_" + env + "_" + service;
    }

    private String buildPath(String service, String instance) {
        return buildServicePath(service) + "/" + instance.replace(":", "_");
    }
}
