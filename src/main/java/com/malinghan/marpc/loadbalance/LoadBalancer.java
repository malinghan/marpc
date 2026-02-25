package com.malinghan.marpc.loadbalance;

import java.util.List;

/**
 * 负载均衡抽象接口，从候选实例列表中选择一个。
 */
public interface LoadBalancer {
    String choose(List<String> instances);
}
