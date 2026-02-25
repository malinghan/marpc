package com.malinghan.marpc.retry;

import lombok.Data;

/**
 * 重试策略配置。
 */
@Data
public class RetryPolicy {

    /** 最大重试次数（不含首次调用），默认 0 表示不重试 */
    private int maxRetries = 0;

    /** 超时时间（毫秒），默认 3000ms */
    private int timeout = 3000;

    /** 是否在重试时切换节点，默认 true */
    private boolean switchInstanceOnRetry = true;

    public static RetryPolicy noRetry() {
        return new RetryPolicy();
    }

    public static RetryPolicy of(int maxRetries, int timeout) {
        RetryPolicy policy = new RetryPolicy();
        policy.setMaxRetries(maxRetries);
        policy.setTimeout(timeout);
        return policy;
    }
}
