package com.malinghan.marpc.loadbalance;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 轮询负载均衡实现。
 */
public class RoundRobinLoadBalancer implements LoadBalancer {

    private final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public String choose(List<String> instances) {
        if (instances == null || instances.isEmpty()) {
            throw new IllegalArgumentException("instances is empty");
        }
        int idx = Math.abs(counter.getAndIncrement() % instances.size());
        return instances.get(idx);
    }
}
