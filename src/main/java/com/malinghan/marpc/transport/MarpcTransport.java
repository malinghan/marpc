package com.malinghan.marpc.transport;

import com.alibaba.fastjson2.JSON;
import com.malinghan.marpc.core.RpcRequest;
import com.malinghan.marpc.core.RpcResponse;
import com.malinghan.marpc.provider.ProviderBootstrap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * RPC 传输层，暴露唯一的 HTTP 端点供 Consumer 调用。
 *
 * <p>所有 RPC 请求统一通过 {@code POST /marpc} 进入，反序列化为 {@link RpcRequest} 后
 * 委托给 {@link ProviderBootstrap} 执行，结果以 {@link RpcResponse} 返回。
 * 传输层本身不含任何业务逻辑，只做协议转换。
 */
@RestController
public class MarpcTransport {

    private final ProviderBootstrap providerBootstrap;

    public MarpcTransport(ProviderBootstrap providerBootstrap) {
        this.providerBootstrap = providerBootstrap;
    }

    /**
     * 接收 RPC 请求并分发给 Provider 执行。
     *
     * @param request 由 Spring MVC 从请求体反序列化的 RPC 请求
     * @return 执行结果，始终返回 HTTP 200，业务异常通过 {@code status=false} 表达
     */
    @PostMapping("/marpc")
    public RpcResponse invoke(@RequestBody RpcRequest request) {
        return providerBootstrap.invoke(request);
    }
}