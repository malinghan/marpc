package com.malinghan.marpc.consumer;

import com.malinghan.marpc.annotation.MarpcConsumer;
import com.malinghan.marpc.circuitbreaker.CircuitBreaker;
import com.malinghan.marpc.circuitbreaker.CircuitBreakerState;
import com.malinghan.marpc.demo.HelloService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 场景8：熔断器
 * - 模拟连续失败触发熔断（CLOSED → OPEN）
 * - 验证熔断后快速失败
 * - 等待半开状态（OPEN → HALF_OPEN）
 * - 探测成功后恢复（HALF_OPEN → CLOSED）
 */
@Slf4j
@Component
public class Scene8CircuitBreaker {

    @MarpcConsumer
    private HelloService helloService;

    private final CircuitBreaker circuitBreaker;

    public Scene8CircuitBreaker(CircuitBreaker circuitBreaker) {
        this.circuitBreaker = circuitBreaker;
    }

    public void run() {
        log.info("=== Scene8: 熔断器 ===");
        log.info("  提示：需要启用熔断器配置（marpc.circuitbreaker.enabled=true）");
        log.info("  配置：faultLimit=5, halfOpenInitialDelay=10000ms");

        log.info("  当前熔断器状态: {}, 失败计数: {}",
                circuitBreaker.getState(), circuitBreaker.getFailureCount());

        if (circuitBreaker.getState() == CircuitBreakerState.CLOSED) {
            log.info("  熔断器处于 CLOSED 状态，正常调用");
            try {
                String result = helloService.hello("circuit-test");
                log.info("  [SUCCESS] 调用成功: {}", result);
            } catch (Exception e) {
                log.error("  [FAIL] 调用失败: {}", e.getMessage());
            }
        } else if (circuitBreaker.getState() == CircuitBreakerState.OPEN) {
            log.warn("  熔断器已打开，调用将快速失败");
            try {
                helloService.hello("circuit-test");
            } catch (Exception e) {
                log.info("  [EXPECTED] 熔断器拦截: {}", e.getMessage());
            }
        } else {
            log.info("  熔断器处于 HALF_OPEN 状态，允许探测请求");
        }

        log.info("=== Scene8 完成 ===\n");
    }
}
