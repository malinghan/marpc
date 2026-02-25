package com.malinghan.marpc.circuitbreaker;

import com.malinghan.marpc.exception.MarpcFrameworkException;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static com.malinghan.marpc.exception.MarpcException.ErrorCode.CIRCUIT_BREAKER_OPEN;

/**
 * 熔断器实现：滑动时间窗口 + 状态机（Closed → Open → Half-Open → Closed）。
 */
@Slf4j
public class CircuitBreaker {

    private final CircuitBreakerConfig config;
    private final AtomicReference<CircuitBreakerState> state = new AtomicReference<>(CircuitBreakerState.CLOSED);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);
    private final AtomicLong openTime = new AtomicLong(0);

    public CircuitBreaker(CircuitBreakerConfig config) {
        this.config = config;
    }

    /**
     * 调用前检查：如果熔断器打开，抛出异常快速失败。
     */
    public void preCall() {
        if (!config.isEnabled()) return;

        CircuitBreakerState currentState = state.get();

        if (currentState == CircuitBreakerState.OPEN) {
            long elapsed = System.currentTimeMillis() - openTime.get();
            if (elapsed >= config.getHalfOpenInitialDelay()) {
                // 进入半开状态
                if (state.compareAndSet(CircuitBreakerState.OPEN, CircuitBreakerState.HALF_OPEN)) {
                    log.info("[CircuitBreaker] 进入 HALF_OPEN 状态，允许探测请求");
                }
            } else {
                throw new MarpcFrameworkException(CIRCUIT_BREAKER_OPEN,
                        "熔断器已打开，快速失败（剩余 " + (config.getHalfOpenInitialDelay() - elapsed) + "ms）");
            }
        }
    }

    /**
     * 调用成功：重置失败计数，Half-Open → Closed。
     */
    public void onSuccess() {
        if (!config.isEnabled()) return;

        CircuitBreakerState currentState = state.get();
        if (currentState == CircuitBreakerState.HALF_OPEN) {
            if (state.compareAndSet(CircuitBreakerState.HALF_OPEN, CircuitBreakerState.CLOSED)) {
                failureCount.set(0);
                log.info("[CircuitBreaker] 探测成功，恢复 CLOSED 状态");
            }
        } else if (currentState == CircuitBreakerState.CLOSED) {
            // 成功调用重置失败计数
            failureCount.set(0);
        }
    }

    /**
     * 调用失败：累计失败次数，达到阈值时 Closed → Open，Half-Open → Open。
     */
    public void onFailure() {
        if (!config.isEnabled()) return;

        long now = System.currentTimeMillis();
        long lastFail = lastFailureTime.get();

        // 滑动窗口：超过窗口时间则重置计数
        if (now - lastFail > config.getWindowSize() * 1000L) {
            failureCount.set(0);
        }

        lastFailureTime.set(now);
        int failures = failureCount.incrementAndGet();

        CircuitBreakerState currentState = state.get();

        if (currentState == CircuitBreakerState.CLOSED && failures >= config.getFaultLimit()) {
            if (state.compareAndSet(CircuitBreakerState.CLOSED, CircuitBreakerState.OPEN)) {
                openTime.set(now);
                log.warn("[CircuitBreaker] 失败次数达到 {}，进入 OPEN 状态", failures);
            }
        } else if (currentState == CircuitBreakerState.HALF_OPEN) {
            if (state.compareAndSet(CircuitBreakerState.HALF_OPEN, CircuitBreakerState.OPEN)) {
                openTime.set(now);
                log.warn("[CircuitBreaker] 探测失败，重新进入 OPEN 状态");
            }
        }
    }

    public CircuitBreakerState getState() {
        return state.get();
    }

    public int getFailureCount() {
        return failureCount.get();
    }

    /** 重置熔断器（用于测试） */
    public void reset() {
        state.set(CircuitBreakerState.CLOSED);
        failureCount.set(0);
        lastFailureTime.set(0);
        openTime.set(0);
    }
}
