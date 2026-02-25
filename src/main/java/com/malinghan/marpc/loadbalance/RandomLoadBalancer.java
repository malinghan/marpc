package com.malinghan.marpc.loadbalance;

import java.util.List;
import java.util.Random;

/**
 * 随机负载均衡实现。
 */
public class RandomLoadBalancer implements LoadBalancer {

    private final Random random = new Random();

    @Override
    public String choose(List<String> instances) {
        if (instances == null || instances.isEmpty()) {
            throw new IllegalArgumentException("instances is empty");
        }
        return instances.get(random.nextInt(instances.size()));
    }
}
