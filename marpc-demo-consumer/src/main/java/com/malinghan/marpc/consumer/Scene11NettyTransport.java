package com.malinghan.marpc.consumer;

import com.malinghan.marpc.annotation.MarpcConsumer;
import com.malinghan.marpc.context.RpcContext;
import com.malinghan.marpc.demo.HelloService;
import com.malinghan.marpc.demo.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 场景11：Netty 传输验证
 * - 验证基本调用（字符串、整数、复杂对象）
 * - 验证并发调用下序列号匹配正确
 * - 验证 Netty + RpcContext 联合使用
 * 切换方式：marpc.transport=netty（默认 okhttp）
 */
@Slf4j
@Component
public class Scene11NettyTransport {

    @MarpcConsumer
    private HelloService helloService;

    public void run() {
        log.info("=== Scene11: Netty 传输验证 ===");
        log.info("  说明：需配置 marpc.transport=netty 启用 Netty 传输");
        log.info("  当前传输方式由 marpc.transport 配置决定，本场景逻辑与传输方式无关");

        // 测试1：基本类型调用
        log.info("\n  [测试1] 基本类型调用");
        try {
            String r1 = helloService.hello("netty");
            log.info("    hello(\"netty\") = {}", r1);
            int r2 = helloService.add(10, 32);
            log.info("    add(10, 32) = {}", r2);
            String r3 = helloService.hello("ha", 3);
            log.info("    hello(\"ha\", 3) = ", r3);
        } catch (Exception e) {
            log.error("    [FAIL] {}", e.getMessage());
        }

        // 测试2：复杂对象调用
        log.info("\n  [测试2] 复杂对象调用");
        try {
            User user = helloService.getUser(42);
            log.info("    getUser(42) = id={}, name={}", user.getId(), user.getName());
            List<String> list = helloService.list("item-", 4);
            log.info("    list(\"item-\", 4) = {}", list);
        } catch (Exception e) {
            log.error("    [FAIL] {}", e.getMessage());
        }

        // 测试3：并发调用（验证序列号匹配）
        log.info("\n  [测试3] 并发调用（10 线程，验证序列号匹配正确）");
        int threads = 10;
        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger fail = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            final int idx = i;
            pool.submit(() -> {
                try {
                    int result = helloService.add(idx, idx);
                    if (result == idx + idx) {
                        success.incrementAndGet();
                    } else {
                        log.error("    [MISMATCH] add({0},{0}) 期望 {1} 实际 {2}", idx, idx * 2, result);
                        fail.incrementAndGet();
                    }
                } catch (Exception e) {
                    log.error("    [FAIL] 线程 {} 调用失败: {}", idx, e.getMessage());
                    fail.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        pool.shutdown();
        log.info("    并发结果: 成功={}, 失败={}", success.get(), fail.get());

        // 测试4：Netty + RpcContext 联合
        log.info("\n  [测试4] Netty 传输 + RpcContext 联合验证");
        RpcContext.setGrayId("netty-gray-001");
        RpcContext.set("traceId", "netty-trace-xyz");
        log.info("    Consumer 设置: grayId=netty-gray-001, traceId=netty-trace-xyz");
        try {
            String result = helloService.hello("netty-context");
            log.info("    调用成功: {}", result);
            log.info("    提示：Provider 日志应输出 grayId=netty-gray-001, traceId=netty-trace-xyz");
        } catch (Exception e) {
            log.error("    [FAIL] {}", e.getMessage());
        }

        log.info("=== Scene11 完成 ===\n");
    }
}
