package com.malinghan.marpc.loadbalance;

import java.util.List;

public interface LoadBalancer {
    String choose(List<String> instances);
}
