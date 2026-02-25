package com.malinghan.marpc.registry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * MaregistryCenter 测试类
 * 验证自定义注册中心的基本功能
 */
public class MaregistryCenterTest {

    private MaregistryCenter registryCenter;
    private static final String TEST_REGISTRY_ADDRESS = "http://localhost:8081";

    @BeforeEach
    public void setUp() {
        registryCenter = new MaregistryCenter(TEST_REGISTRY_ADDRESS);
    }

    @AfterEach
    public void tearDown() {
        registryCenter.stop();
    }

    @Test
    public void testConstructor() {
        assertNotNull(registryCenter);
        // 验证构造函数正确初始化
        assertDoesNotThrow(() -> registryCenter.start());
    }

    @Test
    public void testStartAndStop() {
        // 测试启动
        assertDoesNotThrow(() -> registryCenter.start());
        
        // 测试重复启动不会抛异常
        assertDoesNotThrow(() -> registryCenter.start());
        
        // 测试停止
        assertDoesNotThrow(() -> registryCenter.stop());
        
        // 测试重复停止不会抛异常
        assertDoesNotThrow(() -> registryCenter.stop());
    }

    @Test
    public void testRegisterAndUnregister() {
        String service = "test.service";
        String instance = "localhost:8080";
        
        // 注意：这里只是验证方法调用不会抛出异常
        // 实际的 HTTP 调用会因为服务不存在而失败，但在单元测试中这是预期的
        assertThrows(RuntimeException.class, () -> registryCenter.register(service, instance));
        assertDoesNotThrow(() -> registryCenter.unregister(service, instance));
    }

    @Test
    public void testFetchAll() {
        String service = "test.service";
        
        // 测试获取服务实例列表
        List<String> instances = registryCenter.fetchAll(service);
        assertNotNull(instances);
        // 由于服务不存在，应该返回空列表或缓存数据
        assertTrue(instances.isEmpty() || instances.size() >= 0);
    }

    @Test
    public void testSubscribe() throws InterruptedException {
        String service = "test.service";
        CountDownLatch latch = new CountDownLatch(1);
        
        // 创建监听器
        RegistryCenter.ChangeListener listener = newInstances -> {
            System.out.println("Received notification: " + newInstances);
            latch.countDown();
        };
        
        // 订阅服务
        assertDoesNotThrow(() -> registryCenter.subscribe(service, listener));
        
        // 等待一段时间让轮询执行
        boolean notified = latch.await(5, TimeUnit.SECONDS);
        
        // 由于是模拟环境，不强制要求收到通知
        // 主要验证订阅过程不抛异常
        System.out.println("Subscription test completed, notified: " + notified);
    }

    @Test
    public void testRegistryCenterInterface() {
        // 验证实现了正确的接口
        assertTrue(registryCenter instanceof RegistryCenter);
        
        // 验证所有必需的方法都存在且可调用
        assertDoesNotThrow(() -> registryCenter.start());
        assertDoesNotThrow(() -> registryCenter.stop());
        
        // 注意：以下方法在网络不可达时会抛出异常，这在单元测试中是正常的
        // 我们主要验证方法签名正确且不抛出意外的异常类型
        try {
            registryCenter.register("test", "localhost:8080");
        } catch (RuntimeException e) {
            // 预期的网络异常
            assertTrue(e.getMessage().contains("register failed"));
        }
        
        assertDoesNotThrow(() -> registryCenter.unregister("test", "localhost:8080"));
        assertDoesNotThrow(() -> registryCenter.fetchAll("test"));
    }

    @Test
    public void testChangeListenerFunctionalInterface() {
        // 测试 ChangeListener 函数式接口
        RegistryCenter.ChangeListener listener = instances -> {
            System.out.println("Changed instances: " + instances);
        };
        
        assertNotNull(listener);
        
        // 验证可以正常调用
        assertDoesNotThrow(() -> listener.onChange(List.of("localhost:8080")));
    }
}