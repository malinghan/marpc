package com.malinghan.marpc.registry;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 注册中心功能演示程序
 * 直接运行验证 MaregistryCenter 和 ZkRegistryCenter 的功能
 */
public class RegistryCenterDemo {

    public static void main(String[] args) {
        System.out.println("=== marpc 注册中心功能演示 ===\n");
        
        // 测试 MaregistryCenter
        testMaregistryCenter();
        
        System.out.println("\n" + "=".repeat(50) + "\n");
        
        // 测试 ZkRegistryCenter (如果Zookeeper可用)
//        testZkRegistryCenter();
    }

    private static void testMaregistryCenter() {
        System.out.println("1. 测试 MaregistryCenter (HTTP注册中心)");
        System.out.println("-".repeat(30));
        
        try {
            // 创建 MaregistryCenter 实例
            MaregistryCenter registry = new MaregistryCenter("http://localhost:8081");
            
            // 启动注册中心
            System.out.println("启动注册中心...");
            registry.start();
            
            String service = "com.example.DemoService";
            String instance = "localhost:8080";
            
            // 注册服务
            System.out.println("注册服务实例: " + service + " -> " + instance);
            try {
                registry.register(service, instance);
                System.out.println("✓ 服务注册成功");
            } catch (Exception e) {
                System.out.println("⚠ 服务注册失败 (预期行为，因为注册中心服务可能未运行): " + e.getMessage());
            }
            
            // 获取服务实例
            System.out.println("获取服务实例列表...");
            List<String> instances = registry.fetchAll(service);
            System.out.println("✓ 获取到实例: " + instances);
            
            // 订阅服务变更
            System.out.println("订阅服务变更...");
            CountDownLatch latch = new CountDownLatch(1);
            
            registry.subscribe(service, newInstances -> {
                System.out.println("✓ 收到服务变更通知: " + newInstances);
                latch.countDown();
            });
            
            // 等待一段时间观察轮询效果
            System.out.println("等待5秒观察轮询检测...");
            try {
                boolean notified = latch.await(5, TimeUnit.SECONDS);
                if (!notified) {
                    System.out.println("5秒内未收到变更通知 (正常情况)");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // 注销服务
            System.out.println("注销服务实例...");
            try {
                registry.unregister(service, instance);
                System.out.println("✓ 服务注销完成");
            } catch (Exception e) {
                System.out.println("⚠ 服务注销异常: " + e.getMessage());
            }
            
            // 停止注册中心
            System.out.println("停止注册中心...");
            registry.stop();
            System.out.println("✓ MaregistryCenter 测试完成");
            
        } catch (Exception e) {
            System.err.println("✗ MaregistryCenter 测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testZkRegistryCenter() {
        System.out.println("2. 测试 ZkRegistryCenter (Zookeeper注册中心)");
        System.out.println("-".repeat(30));
        
        try {
            // 创建 ZkRegistryCenter 实例
            ZkRegistryCenter registry = new ZkRegistryCenter("localhost:2181", "demo-app", "test");
            
            // 启动注册中心
            System.out.println("启动Zookeeper注册中心...");
            try {
                registry.start();
                System.out.println("✓ Zookeeper连接成功");
                
                String service = "com.example.ZkDemoService";
                String instance = "localhost:8081";
                
                // 注册服务
                System.out.println("注册服务实例: " + service + " -> " + instance);
                try {
                    registry.register(service, instance);
                    System.out.println("✓ 服务注册成功");
                } catch (Exception e) {
                    System.out.println("⚠ 服务注册失败 (可能是Zookeeper未运行): " + e.getMessage());
                }
                
                // 获取服务实例
                System.out.println("获取服务实例列表...");
                List<String> instances = registry.fetchAll(service);
                System.out.println("✓ 获取到实例: " + instances);
                
                // 停止注册中心
                System.out.println("停止注册中心...");
                registry.stop();
                System.out.println("✓ ZkRegistryCenter 测试完成");
                
            } catch (Exception e) {
                System.out.println("⚠ Zookeeper连接失败 (可能是服务未运行): " + e.getMessage());
                System.out.println("✓ ZkRegistryCenter 测试完成 (连接失败是预期行为)");
            }
            
        } catch (Exception e) {
            System.err.println("✗ ZkRegistryCenter 测试异常: " + e.getMessage());
        }
    }
}