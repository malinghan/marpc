package com.malinghan.marpc.circuitbreaker;

/**
 * 熔断器状态。
 */
public enum CircuitBreakerState {
    /** 关闭状态：正常调用 */
    CLOSED,
    /** 打开状态：快速失败，不发起调用 */
    OPEN,
    /** 半开状态：允许少量探测请求 */
    HALF_OPEN
}
