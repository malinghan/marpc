package com.malinghan.marpc.consumer;

import com.malinghan.marpc.annotation.MarpcConsumer;
import com.malinghan.marpc.demo.HelloService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 场景4：并发调用
 * - 10 个线程并发调用 hello()，验证结果全部正确
 * - 统计成功/失败次数
 */
@Slf4j
@Component
public class Scene4ConcurrentCalls {

    @MarpcConsumer
    private HelloService helloService;

    public void run() throws InterruptedException {
        log.info("=== Scene4: 并发调用 ===");

        int threads = 10;
        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger failure = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            final int idx = i;
            pool.submit(() -> {
                try {
                    String result = helloService.hello("user-" + idx);
                    if (("hello, user-" + idx).equals(result)) {
                        success.incrementAndGet();
                    } else {
                        failure.incrementAndGet();
                        log.warn("  结果不符: expected=hello, user-{} actual={}", idx, result);
                    }
                } catch (Exception e) {
                    failure.incrementAndGet();
                    log.error("  调用失败: thread={}", idx, e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        pool.shutdown();

        check("并发 " + threads + " 线程全部成功",
                failure.get() == 0,
                "success=" + success.get() + " failure=" + failure.get());

        log.info("=== Scene4 完成 ===\n");
    }

    private void check(String desc, boolean ok, String detail) {
        log.info("  [{}] {} | {}", ok ? "PASS" : "FAIL", desc, detail);
    }
}
