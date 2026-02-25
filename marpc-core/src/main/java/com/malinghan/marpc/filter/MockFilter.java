package com.malinghan.marpc.filter;

import com.malinghan.marpc.core.RpcRequest;
import com.malinghan.marpc.core.RpcResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock Filter，拦截指定方法直接返回预设值，用于测试场景。
 *
 * <p>通过 {@link #mock(String, Object)} 注册 mock 规则，
 * key 为方法签名（methodSign），value 为 mock 返回值。
 * 匹配时短路，不发起远程调用。
 */
@Slf4j
public class MockFilter implements Filter {

    private final Map<String, Object> mocks = new ConcurrentHashMap<>();

    /** 注册 mock 规则：methodSign -> mockValue */
    public MockFilter mock(String methodSign, Object value) {
        mocks.put(methodSign, value);
        return this;
    }

    public void clearMocks() {
        mocks.clear();
    }

    @Override
    public RpcResponse preFilter(RpcRequest request) {
        String sign = request.getMethodSign();
        if (sign != null && mocks.containsKey(sign)) {
            Object value = mocks.get(sign);
            log.info("[MockFilter] 拦截请求: {} -> mock={}", sign, value);
            return RpcResponse.ok(value);
        }
        return null;
    }

    @Override
    public void postFilter(RpcRequest request, RpcResponse response) {
        // mock 场景无需后置处理
    }

    @Override
    public int order() {
        return 0; // 最先执行，优先级高于 CacheFilter
    }
}
