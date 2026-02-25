package com.malinghan.marpc.registry;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ZkRegistryCenter 集成测试，使用 Testcontainers 启动隔离的 Zookeeper 容器，
 * 不依赖本地 Zookeeper 环境。
 */
@Testcontainers
class ZkRegistryCenterTest {

    @Container
    static final GenericContainer<?> ZK = new GenericContainer<>(
            DockerImageName.parse("zookeeper:3.8"))
            .withExposedPorts(2181);

    private ZkRegistryCenter registry;
    private static final String SERVICE = "com.malinghan.marpc.demo.HelloService";
    private static final String INSTANCE = "localhost:8080";

    @BeforeEach
    void setUp() {
        String zkAddress = ZK.getHost() + ":" + ZK.getMappedPort(2181);
        registry = new ZkRegistryCenter(zkAddress, "test-app", "test");
        registry.start();
    }

    @AfterEach
    void tearDown() {
        registry.stop();
    }

    @Test
    void register_and_fetchAll() {
        registry.register(SERVICE, INSTANCE);

        List<String> instances = registry.fetchAll(SERVICE);
        assertEquals(1, instances.size());
        assertEquals(INSTANCE, instances.get(0));
    }

    @Test
    void unregister_removesInstance() {
        registry.register(SERVICE, INSTANCE);
        registry.unregister(SERVICE, INSTANCE);

        List<String> instances = registry.fetchAll(SERVICE);
        assertTrue(instances.isEmpty());
    }

    @Test
    void fetchAll_noService_returnsEmpty() {
        List<String> instances = registry.fetchAll("com.example.NoSuchService");
        assertTrue(instances.isEmpty());
    }

    @Test
    void register_multipleInstances() {
        registry.register(SERVICE, "localhost:8080");
        registry.register(SERVICE, "localhost:8081");

        List<String> instances = registry.fetchAll(SERVICE);
        assertEquals(2, instances.size());
        assertTrue(instances.contains("localhost:8080"));
        assertTrue(instances.contains("localhost:8081"));
    }

    @Test
    void subscribe_notifiedOnChange() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<List<String>> received = new AtomicReference<>();

        registry.subscribe(SERVICE, newInstances -> {
            received.set(newInstances);
            latch.countDown();
        });

        registry.register(SERVICE, INSTANCE);

        boolean notified = latch.await(5, TimeUnit.SECONDS);
        assertTrue(notified, "订阅回调未在 5 秒内触发");
        assertNotNull(received.get());
    }
}
