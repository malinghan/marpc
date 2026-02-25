package com.malinghan.marpc.filter;

import com.malinghan.marpc.core.RpcRequest;
import com.malinghan.marpc.core.RpcResponse;

/**
 * Filter 接口，支持在 RPC 调用前后插入自定义逻辑。
 *
 * <p>执行顺序由 {@link #order()} 决定，数值越小越先执行。
 * preFilter 返回非 null 时直接短路，跳过后续 Filter 和远程调用。
 */
public interface Filter {

    /**
     * 调用前置处理。
     * @return null 继续执行链；非 null 直接作为最终响应返回（短路）
     */
    RpcResponse preFilter(RpcRequest request);

    /**
     * 调用后置处理，可修改或记录响应。
     */
    void postFilter(RpcRequest request, RpcResponse response);

    /**
     * 执行顺序，数值越小越先执行。默认 0。
     */
    default int order() {
        return 0;
    }
}
