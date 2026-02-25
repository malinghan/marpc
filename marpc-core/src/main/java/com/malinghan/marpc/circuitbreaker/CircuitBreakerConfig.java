package com.malinghan.marpc.circuitbreaker;

import lombok.Data;

/**
 * 熔断器配置。
 */
@Data
public class CircuitBreakerConfig {

    /** 是否启用熔断器，默认 false */
    private boolean enabled = false;

    /** 触发熔断的失败次数阈值，默认 5 */
    private int faultLimit = 5;

    /** 熔断后首次进入半开状态的延迟（毫秒），默认 10000ms */
    private long halfOpenInitialDelay = 10000;

    /** 半开状态下探测失败后再次进入半开的延迟（毫秒），默认 5000ms */
    private long halfOpenDelay = 5000;

    /** 滑动窗口大小（秒），默认 10 秒 */
    private int windowSize = 10;

    public static CircuitBreakerConfig disabled() {
        return new CircuitBreakerConfig();
    }

    public static CircuitBreakerConfig of(int faultLimit, long halfOpenInitialDelay, long halfOpenDelay) {
        CircuitBreakerConfig config = new CircuitBreakerConfig();
        config.setEnabled(true);
        config.setFaultLimit(faultLimit);
        config.setHalfOpenInitialDelay(halfOpenInitialDelay);
        config.setHalfOpenDelay(halfOpenDelay);
        return config;
    }
}
