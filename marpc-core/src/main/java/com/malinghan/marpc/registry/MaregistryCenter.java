package com.malinghan.marpc.registry;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.malinghan.marpc.exception.MarpcFrameworkException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.malinghan.marpc.exception.MarpcException.ErrorCode.*;

/**
 * 基于 maregistry HTTP API 的注册中心实现
 *
 * 接口协议：
 *   POST /reg?service=xxx          body: InstanceMeta JSON  -> 注册
 *   POST /unreg?service=xxx        body: InstanceMeta JSON  -> 注销
 *   GET  /findAll?service=xxx                               -> 查询实例列表
 *   POST /renews?services=a,b,c    body: InstanceMeta JSON  -> 批量心跳续约
 *   GET  /version?service=xxx                               -> 获取服务版本号
 */
@Slf4j
public class MaregistryCenter implements RegistryCenter {

    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");
    private static final int HEARTBEAT_INTERVAL_SECONDS = 5;
    private static final int POLL_INTERVAL_SECONDS = 5;

    private final String registryAddress;
    private final OkHttpClient httpClient;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    // service -> 已知版本号，用于变更检测
    private final Map<String, Long> versionCache = new ConcurrentHashMap<>();
    // service -> 已知实例列表缓存
    private final Map<String, List<String>> instanceCache = new ConcurrentHashMap<>();
    // 已注册的实例（用于心跳续约）：service -> InstanceMeta JSON
    private final Map<String, String> registeredInstances = new ConcurrentHashMap<>();

    public MaregistryCenter(String registryAddress) {
        this.registryAddress = registryAddress.endsWith("/")
                ? registryAddress.substring(0, registryAddress.length() - 1)
                : registryAddress;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public void start() {
        log.info("[MaregistryCenter] 已连接注册中心: {}", registryAddress);
    }

    @Override
    public void stop() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
        log.info("[MaregistryCenter] 已关闭");
    }

    /**
     * 注册服务实例
     * POST /reg?service=xxx  body: {"scheme":"http","host":"...","port":8080,"context":""}
     *
     * @param service  服务名，如 com.example.UserService
     * @param instance 实例地址，格式 host:port
     */
    @Override
    public void register(String service, String instance) {
        try {
            String body = toInstanceMetaJson(instance);
            String url = registryAddress + "/reg?service=" + service;
            post(url, body);
            registeredInstances.put(service, body);
            log.info("[MaregistryCenter] 注册成功: {} -> {}", service, instance);
            startHeartbeat();
        } catch (Exception e) {
            throw new MarpcFrameworkException(PROVIDER_REGISTER_FAILED,
                    "register failed: " + service, e);
        }
    }

    /**
     * 注销服务实例
     * POST /unreg?service=xxx  body: InstanceMeta JSON
     */
    @Override
    public void unregister(String service, String instance) {
        try {
            String body = toInstanceMetaJson(instance);
            String url = registryAddress + "/unreg?service=" + service;
            post(url, body);
            registeredInstances.remove(service);
            log.info("[MaregistryCenter] 注销成功: {} -> {}", service, instance);
        } catch (Exception e) {
            log.error("[MaregistryCenter] 注销失败: {} -> {}", service, instance, e);
        }
    }

    /**
     * 查询服务所有实例
     * GET /findAll?service=xxx  -> List<InstanceMeta>
     * 返回格式转换为 host:port 字符串列表
     */
    @Override
    public List<String> fetchAll(String service) {
        try {
            String url = registryAddress + "/findAll?service=" + service;
            String responseBody = get(url);
            List<Map<String, Object>> metas = JSON.parseObject(responseBody,
                    new TypeReference<>() {});
            List<String> instances = metas.stream()
                    .map(m -> m.get("host") + ":" + m.get("port"))
                    .collect(Collectors.toList());
            instanceCache.put(service, instances);
            return instances;
        } catch (Exception e) {
            log.warn("[MaregistryCenter] fetchAll 失败: {}, 使用缓存", service, e);
            return instanceCache.getOrDefault(service, List.of());
        }
    }

    /**
     * 订阅服务变更
     * 基于 GET /version?service=xxx 轮询版本号，版本变化时拉取最新实例列表
     */
    @Override
    public void subscribe(String service, ChangeListener listener) {
        // 初始化版本缓存
        versionCache.put(service, fetchVersion(service));
        log.info("[MaregistryCenter] 订阅服务: {}", service);

        scheduler.scheduleWithFixedDelay(() -> {
            try {
                long latestVersion = fetchVersion(service);
                long knownVersion = versionCache.getOrDefault(service, -1L);
                if (latestVersion != knownVersion) {
                    log.info("[MaregistryCenter] 服务版本变更: {} v{} -> v{}", service, knownVersion, latestVersion);
                    versionCache.put(service, latestVersion);
                    List<String> newInstances = fetchAll(service);
                    listener.onChange(newInstances);
                }
            } catch (Exception e) {
                log.error("[MaregistryCenter] 轮询版本失败: {}", service, e);
            }
        }, POLL_INTERVAL_SECONDS, POLL_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    // ---- 私有方法 ----

    private void startHeartbeat() {
        // 只启动一次（幂等：scheduler 已有任务时不重复提交）
        if (registeredInstances.isEmpty()) return;
        scheduler.scheduleWithFixedDelay(() -> {
            if (registeredInstances.isEmpty()) return;
            try {
                String services = String.join(",", registeredInstances.keySet());
                // 取第一个实例的 body（provider 通常只注册一个实例）
                String body = registeredInstances.values().iterator().next();
                String url = registryAddress + "/renews?services=" + services;
                post(url, body);
                log.debug("[MaregistryCenter] 心跳续约: {}", services);
            } catch (Exception e) {
                log.warn("[MaregistryCenter] 心跳续约失败", e);
            }
        }, HEARTBEAT_INTERVAL_SECONDS, HEARTBEAT_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private long fetchVersion(String service) {
        try {
            String url = registryAddress + "/version?service=" + service;
            String body = get(url);
            return Long.parseLong(body.trim());
        } catch (Exception e) {
            log.warn("[MaregistryCenter] 获取版本失败: {}", service, e);
            return -1L;
        }
    }

    private void post(String url, String jsonBody) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(jsonBody, JSON_TYPE))
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code() + " " + response.message());
            }
        }
    }

    private String get(String url) throws IOException {
        Request request = new Request.Builder().url(url).get().build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code() + " " + response.message());
            }
            return response.body() != null ? response.body().string() : "[]";
        }
    }

    /**
     * 将 host:port 格式的实例地址转换为 maregistry 要求的 InstanceMeta JSON
     */
    private String toInstanceMetaJson(String instance) {
        String[] parts = instance.split(":");
        String host = parts[0];
        int port = Integer.parseInt(parts[1]);
        return String.format("{\"scheme\":\"http\",\"host\":\"%s\",\"port\":%d,\"context\":\"\"}", host, port);
    }
}