package com.malinghan.marpc.filter;

import com.malinghan.marpc.core.RpcRequest;
import com.malinghan.marpc.core.RpcResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Consumer 端结果缓存 Filter。
 *
 * <p>缓存 key = service + "#" + methodSign + "#" + args。
 * preFilter 命中缓存时直接返回，跳过远程调用。
 * postFilter 将成功响应写入缓存。
 */
@Slf4j
public class CacheFilter implements Filter {

    private final Map<String, RpcResponse> cache = new ConcurrentHashMap<>();

    @Override
    public RpcResponse preFilter(RpcRequest request) {
        String key = buildKey(request);
        RpcResponse cached = cache.get(key);
        if (cached != null) {
            log.info("[CacheFilter] 命中缓存: {}", key);
            return cached;
        }
        return null;
    }

    @Override
    public void postFilter(RpcRequest request, RpcResponse response) {
        if (response.isStatus()) {
            String key = buildKey(request);
            cache.put(key, response);
            log.info("[CacheFilter] 写入缓存: {}", key);
        }
    }

    @Override
    public int order() {
        return 10;
    }

    /** 清空缓存，用于测试 */
    public void clear() {
        cache.clear();
    }

    public int size() {
        return cache.size();
    }

    private String buildKey(RpcRequest request) {
        return request.getService() + "#"
                + request.getMethodSign() + "#"
                + Arrays.toString(request.getArgs());
    }
}
